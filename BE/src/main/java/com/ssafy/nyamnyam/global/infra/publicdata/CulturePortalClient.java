package com.ssafy.nyamnyam.global.infra.publicdata;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 한국문화정보원 한눈에보는문화정보(공공데이터포털 B553457/cultureinfo) 클라이언트. 응답은 XML이다.
 * 목록(realm2)에는 원문 링크가 없어 새 항목만 상세(detail2)를 불러 예매·안내 주소와 소개를 받는다.
 * 서비스키는 공공데이터포털 Decoding 키를 URI 변수로 넘겨 인코딩되게 한다.
 */
@Component
public class CulturePortalClient {

    private static final DateTimeFormatter BASIC = DateTimeFormatter.BASIC_ISO_DATE;

    private final RestClient restClient;
    private final String serviceKey;

    public CulturePortalClient(
            @Value("${app.public-data.culture-portal.base-url:https://apis.data.go.kr/B553457/cultureinfo}") String baseUrl,
            @Value("${app.public-data.culture-portal.key:}") String serviceKey
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

    /** 목록 1건. realmName은 '전시', '음악/콘서트', '교육/체험' 같은 분야 이름 */
    public record Event(String seq, String title, LocalDate start, LocalDate end, String place, String realmName,
                        String sigungu, String thumbnailUrl) {
    }

    public record EventPage(List<Event> items, int totalCount) {
    }

    /** 상세. url = 예매·안내 페이지, placeUrl = 장소 홈페이지, contents = 소개 */
    public record EventDetail(String url, String placeUrl, String contents) {
    }

    /** 전시 분야 코드(realm2 realmCode). 응답 realmName은 '전시' */
    public static final String REALM_EXHIBITION = "D000";

    /** 시도(예: 서울)에서 기간 안에 열리는 한 분야(예: 전시 D000)의 목록(realm2) */
    public EventPage searchByRealm(String sido, String realmCode, LocalDate from, LocalDate to, int pageNo, int numOfRows) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("serviceKey", serviceKey);
        vars.put("sido", sido);
        vars.put("realmCode", realmCode);
        vars.put("from", BASIC.format(from));
        vars.put("to", BASIC.format(to));
        vars.put("pageNo", pageNo);
        vars.put("rows", numOfRows);
        Document doc = get("/realm2?serviceKey={serviceKey}&sido={sido}&realmCode={realmCode}&from={from}&to={to}"
                + "&PageNo={pageNo}&numOfrows={rows}", vars);
        List<Event> items = new ArrayList<>();
        for (Element item : items(doc)) {
            items.add(new Event(text(item, "seq"), text(item, "title"), date(text(item, "startDate")),
                    date(text(item, "endDate")), text(item, "place"), text(item, "realmName"),
                    text(item, "sigungu"), text(item, "thumbnail")));
        }
        return new EventPage(items, intText(doc.getDocumentElement(), "totalCount"));
    }

    public Optional<EventDetail> detail(String seq) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("serviceKey", serviceKey);
        vars.put("seq", seq);
        List<Element> list = items(get("/detail2?serviceKey={serviceKey}&seq={seq}", vars));
        if (list.isEmpty()) return Optional.empty();
        Element item = list.get(0);
        return Optional.of(new EventDetail(url(text(item, "url")), url(text(item, "placeUrl")),
                TourApiClient.stripTags(text(item, "contents1"))));
    }

    private Document get(String uri, Map<String, Object> vars) {
        byte[] xml;
        try {
            xml = restClient.get().uri(uri, vars).retrieve().body(byte[].class);
        } catch (RuntimeException e) {
            throw new PublicDataApiException("문화포털 호출 실패: " + e.getClass().getSimpleName());
        }
        Document doc = parse(xml);
        String code = text(doc.getDocumentElement(), "resultCode");
        if (!"00".equals(code)) {
            // 키 오류는 OpenAPI_ServiceResponse 형식으로 와서 resultCode가 없다
            throw new PublicDataApiException("문화포털 오류 응답: " + (code == null ? text(doc.getDocumentElement(), "returnReasonCode") : code));
        }
        return doc;
    }

    static Document parse(byte[] xml) {
        if (xml == null || xml.length == 0) throw new PublicDataApiException("문화포털 빈 응답");
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        } catch (Exception e) {
            throw new PublicDataApiException("문화포털 응답 해석 실패: " + e.getClass().getSimpleName());
        }
    }

    static List<Element> items(Document doc) {
        NodeList nodes = doc.getElementsByTagName("item");
        List<Element> list = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) list.add((Element) nodes.item(i));
        return list;
    }

    static String text(Element element, String tag) {
        NodeList nodes = element.getElementsByTagName(tag);
        if (nodes.getLength() == 0) return null;
        String value = nodes.item(0).getTextContent();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static int intText(Element element, String tag) {
        try {
            String value = text(element, tag);
            return value == null ? 0 : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static LocalDate date(String yyyymmdd) {
        if (yyyymmdd == null || yyyymmdd.length() != 8) return null;
        try {
            return LocalDate.parse(yyyymmdd, BASIC);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String url(String value) {
        if (value == null) return null;
        String url = value.trim();
        if (url.startsWith("www.")) url = "https://" + url;
        return url.startsWith("http://") || url.startsWith("https://") ? url : null;
    }
}
