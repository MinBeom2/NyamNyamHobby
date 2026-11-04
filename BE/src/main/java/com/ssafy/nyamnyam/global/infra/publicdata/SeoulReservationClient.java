package com.ssafy.nyamnyam.global.infra.publicdata;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 서울 열린데이터광장 '서울시 교육 공공서비스예약 정보'(ListPublicReservationEducation) 클라이언트.
 * 인증키가 주소 경로에 들어가는 API라, 오류 메시지에는 주소를 넣지 않는다.
 * 한 번에 최대 1,000건까지 받을 수 있다(시작·끝 번호로 범위를 정함).
 */
@Component
public class SeoulReservationClient {

    public static final int MAX_ROWS = 1000;

    private final RestClient restClient;
    private final String apiKey;

    public SeoulReservationClient(
            @Value("${app.public-data.seoul-open-api.base-url:http://openapi.seoul.go.kr:8088}") String baseUrl,
            @Value("${app.public-data.seoul-open-api.key:}") String apiKey
    ) {
        this.apiKey = apiKey;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(20));
        this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * 강좌 1건.
     *
     * @param status   접수 상태(접수중, 예약마감, 안내중 등)
     * @param category 소분류(역사, 체육, 미술 등)
     * @param district 자치구 이름(예: 종로구)
     */
    public record Course(String id, String title, String category, String status, String fee, String place,
                         String target, String url, String imageUrl, String district,
                         LocalDate start, LocalDate end) {
    }

    public record CoursePage(List<Course> items, int totalCount) {
    }

    /** start~end번째 강좌(1부터, 끝 번호 포함) */
    public CoursePage list(int start, int end) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("key", apiKey);
        vars.put("start", start);
        vars.put("end", end);
        JsonNode root;
        try {
            root = restClient.get().uri("/{key}/json/ListPublicReservationEducation/{start}/{end}/", vars)
                    .retrieve().body(JsonNode.class);
        } catch (RuntimeException e) {
            throw new PublicDataApiException("서울 공공서비스예약 호출 실패: " + e.getClass().getSimpleName());
        }
        if (root == null) throw new PublicDataApiException("서울 공공서비스예약 빈 응답");
        JsonNode service = root.path("ListPublicReservationEducation");
        // 인증키 오류·데이터 없음은 서비스 이름 없이 최상위 RESULT로만 온다
        String code = (service.isMissingNode() ? root : service).path("RESULT").path("CODE").asString(null);
        if ("INFO-200".equals(code)) return new CoursePage(List.of(), 0); // 해당 데이터 없음
        if (!"INFO-000".equals(code)) {
            throw new PublicDataApiException("서울 공공서비스예약 오류 응답: " + code);
        }
        List<Course> items = new ArrayList<>();
        for (JsonNode row : service.path("row")) {
            items.add(new Course(text(row, "SVCID"), unescape(text(row, "SVCNM")), text(row, "MINCLASSNM"),
                    text(row, "SVCSTATNM"), text(row, "PAYATNM"), unescape(text(row, "PLACENM")),
                    unescape(text(row, "USETGTINFO")), text(row, "SVCURL"), text(row, "IMGURL"),
                    text(row, "AREANM"), date(text(row, "SVCOPNBGNDT")), date(text(row, "SVCOPNENDDT"))));
        }
        return new CoursePage(items, service.path("list_total_count").asInt(0));
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return null;
        String text = value.asString().trim();
        return text.isEmpty() ? null : text;
    }

    /** '2026-08-07 00:00:00.0' 같은 값에서 날짜만 */
    static LocalDate date(String value) {
        if (value == null || value.length() < 10) return null;
        try {
            return LocalDate.parse(value.substring(0, 10));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** 제목에 '&#39;', '&amp;' 같은 HTML 문자 참조가 그대로 들어 있어 화면용 글자로 되돌린다 */
    static String unescape(String value) {
        return HtmlText.unescape(value);
    }
}
