package com.ssafy.nyamnyam.global.infra.kakao;

import com.ssafy.nyamnyam.global.infra.publicdata.PublicDataApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Slf4j
@Component
public class KakaoLocalClient {

    private final RestClient restClient;
    private final String apiKey;

    public KakaoLocalClient(
            @Value("${app.public-data.kakao-local.base-url:https://dapi.kakao.com}") String baseUrl,
            @Value("${app.public-data.kakao-local.key:}") String apiKey
    ) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    /**
     * 카카오 로컬 키워드로 장소 검색 API 호출 (GET /v2/local/search/keyword.json)
     *
     * @param query 검색 키워드 (예: "마포구 클라이밍")
     * @param page  결과 페이지 번호 (1~45)
     * @param size  한 페이지에 보여질 문서의 개수 (1~15)
     * @return 키워드 검색 결과
     */
    public KakaoKeywordSearchResponse searchByKeyword(String query, int page, int size) {
        if (!isConfigured()) {
            throw new PublicDataApiException("[KAKAO_LOCAL] Kakao Local REST API key is not configured");
        }
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/local/search/keyword.json")
                            .queryParam("query", query)
                            .queryParam("page", page)
                            .queryParam("size", size)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + apiKey)
                    .retrieve()
                    .body(KakaoKeywordSearchResponse.class);
        } catch (Exception e) {
            log.error("Failed to call Kakao Local search API for query: {}", query, e);
            throw new PublicDataApiException("[KAKAO_LOCAL] Kakao search failed: " + e.getMessage(), e);
        }
    }

    /**
     * 카카오 Daum 이미지 검색 API 호출 (GET /v2/search/image)
     * 장소명/키워드로 첫 번째 이미지 썸네일 URL을 조회한다.
     *
     * @param query 검색 키워드 (예: "마포구 코알라클라이밍짐")
     * @return 첫 번째 이미지의 thumbnailUrl 또는 imageUrl (실패/미존재 시 null)
     */
    public String searchFirstImageUrl(String query) {
        if (!isConfigured() || query == null || query.isBlank()) {
            return null;
        }
        try {
            KakaoImageSearchResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/search/image")
                            .queryParam("query", query)
                            .queryParam("page", 1)
                            .queryParam("size", 1)
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + apiKey)
                    .retrieve()
                    .body(KakaoImageSearchResponse.class);

            if (response != null && response.documents() != null && !response.documents().isEmpty()) {
                KakaoImageSearchResponse.KakaoImageDocument firstDoc = response.documents().get(0);
                if (firstDoc.thumbnailUrl() != null && !firstDoc.thumbnailUrl().isBlank()) {
                    return firstDoc.thumbnailUrl();
                }
                return firstDoc.imageUrl();
            }
        } catch (Exception e) {
            log.warn("[KAKAO_IMAGE] Failed to fetch image for query '{}': {}", query, e.getMessage());
        }
        return null;
    }
}
