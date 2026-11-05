package com.ssafy.nyamnyam.global.infra.publicdata;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 한국관광공사 국문 관광정보 서비스(TourAPI, KorService2) 클라이언트.
 * KorService2부터 지역은 areaCode 대신 법정동 코드(lDongRegnCd, 서울=11)로 거른다.
 * 서비스키는 공공데이터포털 Decoding 키를 쓰고, URI 변수로 넘겨 '+', '/' 같은 문자까지 인코딩되게 한다.
 */
@Slf4j
@Component
public class TourApiClient {

    private static final DateTimeFormatter BASIC = DateTimeFormatter.BASIC_ISO_DATE;
    private static final Pattern HREF = Pattern.compile("href=[\"']([^\"']+)[\"']");

    private final RestClient restClient;
    private final String serviceKey;

    public TourApiClient(
            @Value("${app.public-data.tour-api.base-url:https://apis.data.go.kr/B551011/KorService2}") String baseUrl,
            @Value("${app.public-data.tour-api.key:}") String serviceKey
    ) {
        this.serviceKey = serviceKey;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    public boolean isConfigured() {
        return serviceKey != null && !serviceKey.isBlank();
    }

    /** 행사 목록 1건 */
    public record Festival(String contentId, String title, LocalDate start, LocalDate end, String imageUrl,
                           String regionCode) {
    }

    /** 행사 목록 한 페이지 */
    public record FestivalPage(List<Festival> items, int totalCount) {
    }

    /** 행사 소개와 공식 홈페이지 */
    public record FestivalDetail(String overview, String homepage) {
    }

    /**
     * 기준일에 진행 중이거나 그 뒤에 열리는 행사(searchFestival2). eventStartDate는 '이 날짜 이후 시작'이 아니라
     * 이 날짜에 끝나지 않은 행사를 모두 준다.
     *
     * @param lDongRegnCd 법정동 시도 코드(서울 11)
     */
    public FestivalPage searchFestivals(String lDongRegnCd, LocalDate from, int pageNo, int numOfRows) {
        Map<String, Object> vars = common();
        vars.put("lDongRegnCd", lDongRegnCd);
        vars.put("eventStartDate", BASIC.format(from));
        vars.put("pageNo", pageNo);
        vars.put("numOfRows", numOfRows);
        JsonNode body = get("/searchFestival2?serviceKey={serviceKey}&MobileOS={os}&MobileApp={app}&_type=json"
                + "&lDongRegnCd={lDongRegnCd}&eventStartDate={eventStartDate}&pageNo={pageNo}&numOfRows={numOfRows}", vars);
        List<Festival> items = new ArrayList<>();
        for (JsonNode item : items(body)) {
            String sido = text(item, "lDongRegnCd");
            String sigungu = text(item, "lDongSignguCd");
            items.add(new Festival(text(item, "contentid"), text(item, "title"),
                    date(text(item, "eventstartdate")), date(text(item, "eventenddate")),
                    blankToNull(text(item, "firstimage")),
                    sido == null || sigungu == null ? null : sido + sigungu));
        }
        return new FestivalPage(items, body.path("totalCount").asInt(0));
    }

    /** 소개(overview)와 공식 홈페이지(detailCommon2). 소개의 HTML 태그와 홈페이지 a 태그는 걷어 낸다 */
    public Optional<FestivalDetail> detail(String contentId) {
        Map<String, Object> vars = common();
        vars.put("contentId", contentId);
        JsonNode body = get("/detailCommon2?serviceKey={serviceKey}&MobileOS={os}&MobileApp={app}&_type=json"
                + "&contentId={contentId}", vars);
        List<JsonNode> list = items(body);
        if (list.isEmpty()) return Optional.empty();
        JsonNode item = list.get(0);
        return Optional.of(new FestivalDetail(stripTags(text(item, "overview")), homepage(text(item, "homepage"))));
    }

    private Map<String, Object> common() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("serviceKey", serviceKey);
        vars.put("os", "ETC");
        vars.put("app", "nyamnyam");
        return vars;
    }

    private JsonNode get(String uri, Map<String, Object> vars) {
        JsonNode root;
        try {
            root = restClient.get().uri(uri, vars).retrieve().body(JsonNode.class);
        } catch (RuntimeException e) {
            // 키 오류 등은 JSON 대신 XML로 오기도 해서 파싱 단계에서 실패한다. 키가 메시지에 남지 않게 원인 종류만 남긴다
            throw new PublicDataApiException("TourAPI 호출 실패: " + e.getClass().getSimpleName());
        }
        JsonNode response = root == null ? null : root.path("response");
        String code = response == null ? null : response.path("header").path("resultCode").asString(null);
        if (code == null || !"0000".equals(code)) {
            throw new PublicDataApiException("TourAPI 오류 응답: " + code);
        }
        return response.path("body");
    }

    /** items가 빈 문자열이거나, 결과가 1건이면 배열 대신 객체로 오는 경우까지 맞춘다 */
    private static List<JsonNode> items(JsonNode body) {
        JsonNode item = body.path("items").path("item");
        List<JsonNode> list = new ArrayList<>();
        if (item.isArray()) item.forEach(list::add);
        else if (item.isObject()) list.add(item);
        return list;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : blankToNull(value.asString());
    }

    private static LocalDate date(String yyyymmdd) {
        if (yyyymmdd == null || yyyymmdd.length() != 8) return null;
        try {
            return LocalDate.parse(yyyymmdd, BASIC);
        } catch (RuntimeException e) {
            return null;
        }
    }

    static String homepage(String html) {
        if (html == null) return null;
        Matcher matcher = HREF.matcher(html);
        String url = matcher.find() ? matcher.group(1) : stripTags(html);
        if (url == null) return null;
        url = url.trim();
        if (url.startsWith("www.")) url = "https://" + url;
        return url.startsWith("http://") || url.startsWith("https://") ? url : null;
    }

    static String stripTags(String html) {
        return HtmlText.plain(html);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
