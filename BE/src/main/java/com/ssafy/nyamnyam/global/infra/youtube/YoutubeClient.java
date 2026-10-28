package com.ssafy.nyamnyam.global.infra.youtube;

import com.ssafy.nyamnyam.global.infra.youtube.dto.YoutubeSearchResponse;
import com.ssafy.nyamnyam.global.infra.youtube.dto.YoutubeVideoListResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * YouTube Data API v3 클라이언트.
 * API 키는 URL이 아니라 X-Goog-Api-Key 헤더로 보내 로그·예외 메시지에 남지 않게 한다.
 * 할당량: search.list 100단위, videos.list 1단위(호출당, 최대 50개).
 */
@Slf4j
@Component
public class YoutubeClient {

    public static final int SEARCH_COST = 100;
    public static final int VIDEOS_COST = 1;
    public static final int MAX_RESULTS = 50;

    private static final Pattern REASON_PATTERN = Pattern.compile("\"reason\"\\s*:\\s*\"([^\"]+)\"");

    private final RestClient restClient;
    private final String apiKey;

    public YoutubeClient(
            @Value("${app.youtube.base-url:https://www.googleapis.com/youtube/v3}") String baseUrl,
            @Value("${app.youtube.api-key:}") String apiKey
    ) {
        this.apiKey = apiKey;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * 쇼츠 길이(4분 미만)·한국·한국어 위주·안전 검색 조건으로 키워드 검색. 100단위.
     *
     * @param pageToken 이어서 볼 페이지. 처음부터 보려면 null
     */
    public YoutubeSearchPage searchShorts(String query, String pageToken) {
        return searchShorts(query, pageToken, null);
    }

    /**
     * @param publishedAfter 이 시각 이후 업로드된 영상만(갱신 검색용). 제한 없으면 null
     */
    public YoutubeSearchPage searchShorts(String query, String pageToken, Instant publishedAfter) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("q", query);
        if (pageToken != null && !pageToken.isBlank()) {
            vars.put("pageToken", pageToken);
        }
        if (publishedAfter != null) {
            // RFC 3339(예: 2026-09-26T03:00:00Z)
            vars.put("publishedAfter", publishedAfter.truncatedTo(ChronoUnit.SECONDS).toString());
        }

        YoutubeSearchResponse body = restClient.get()
                .uri(builder -> {
                    builder.path("/search")
                            .queryParam("part", "id")
                            .queryParam("type", "video")
                            .queryParam("videoDuration", "short")
                            .queryParam("regionCode", "KR")
                            .queryParam("relevanceLanguage", "ko")
                            .queryParam("safeSearch", "strict")
                            .queryParam("maxResults", MAX_RESULTS)
                            .queryParam("q", "{q}");
                    if (vars.containsKey("pageToken")) {
                        builder.queryParam("pageToken", "{pageToken}");
                    }
                    if (vars.containsKey("publishedAfter")) {
                        builder.queryParam("publishedAfter", "{publishedAfter}");
                    }
                    return builder.build(vars);
                })
                .header("X-Goog-Api-Key", apiKey)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .body(YoutubeSearchResponse.class);

        if (body == null || body.items() == null) {
            return new YoutubeSearchPage(List.of(), null);
        }
        List<String> videoIds = body.items().stream()
                .map(YoutubeSearchResponse.Item::id)
                .filter(Objects::nonNull)
                .map(YoutubeSearchResponse.Id::videoId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();
        return new YoutubeSearchPage(videoIds, body.nextPageToken());
    }

    /** 영상 상세(길이·공개 상태·퍼가기·지역 제한·조회수). 50개씩 나눠 부르고, 호출당 1단위. */
    public List<YoutubeVideo> getVideos(List<String> videoIds) {
        List<YoutubeVideo> result = new ArrayList<>();
        for (int from = 0; from < videoIds.size(); from += MAX_RESULTS) {
            String ids = String.join(",", videoIds.subList(from, Math.min(from + MAX_RESULTS, videoIds.size())));

            YoutubeVideoListResponse body = restClient.get()
                    .uri(builder -> builder.path("/videos")
                            .queryParam("part", "snippet,contentDetails,status,statistics")
                            .queryParam("maxResults", MAX_RESULTS)
                            .queryParam("id", "{ids}")
                            .build(Map.of("ids", ids)))
                    .header("X-Goog-Api-Key", apiKey)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, this::handleError)
                    .body(YoutubeVideoListResponse.class);

            if (body != null && body.items() != null) {
                body.items().stream()
                        .filter(item -> item != null && item.id() != null)
                        .map(YoutubeVideoMapper::toVideo)
                        .forEach(result::add);
            }
        }
        return result;
    }

    /** videos.list 호출 횟수 = 필요한 할당량. */
    public static int videosCost(int videoCount) {
        return (videoCount + MAX_RESULTS - 1) / MAX_RESULTS * VIDEOS_COST;
    }

    private void handleError(HttpRequest request, ClientHttpResponse response) throws IOException {
        String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        String reason = extractReason(body);
        int status = response.getStatusCode().value();

        if (status == 403 && ("quotaExceeded".equals(reason) || "dailyLimitExceeded".equals(reason))) {
            throw new YoutubeQuotaExceededException(reason);
        }
        throw new YoutubeApiException("YouTube API 오류 " + status + " (" + reason + ") " + request.getURI().getPath());
    }

    static String extractReason(String body) {
        if (body == null) {
            return "unknown";
        }
        Matcher matcher = REASON_PATTERN.matcher(body);
        return matcher.find() ? matcher.group(1) : "unknown";
    }
}
