package com.ssafy.nyamnyam.global.infra.publicdata;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * KOPIS(공연예술통합전산망) 공연 목록 클라이언트. 응답은 XML이다.
 * 목록 응답에 상세 주소가 없어 공연 ID(mt20id)로 KOPIS 공연 상세 페이지 주소를 만든다.
 */
@Slf4j
@Component
public class KopisClient {

    private static final DateTimeFormatter BASIC = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter DOTTED = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final Pattern TITLE = Pattern.compile("(?is)<title>(.*?)</title>");
    public static final String DETAIL_URL = "https://www.kopis.or.kr/por/db/pblprfr/pblprfrView.do?menuId=MNU_00020&mt20Id=";

    private final RestClient restClient;
    private final String serviceKey;

    public KopisClient(
            @Value("${app.public-data.kopis.base-url:http://www.kopis.or.kr/openApi/restful}") String baseUrl,
            @Value("${app.public-data.kopis.key:}") String serviceKey
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

    /** 공연 목록 1건 */
    public record Performance(String id, String title, LocalDate start, LocalDate end, String posterUrl,
                              String venue, String genre) {
    }

    /**
     * 기간 안에 열리는 공연을 공연명 검색어로 찾는다.
     *
     * @param signguCode 시도 코드(서울 11)
     */
    public List<Performance> search(String keyword, String signguCode, LocalDate from, LocalDate to, int page, int rows) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("service", serviceKey);
        vars.put("stdate", BASIC.format(from));
        vars.put("eddate", BASIC.format(to));
        vars.put("cpage", page);
        vars.put("rows", rows);
        vars.put("signgucode", signguCode);
        vars.put("shprfnm", keyword);
        byte[] xml;
        try {
            xml = restClient.get()
                    .uri("/pblprfr?service={service}&stdate={stdate}&eddate={eddate}&cpage={cpage}&rows={rows}"
                            + "&signgucode={signgucode}&shprfnm={shprfnm}", vars)
                    .retrieve().body(byte[].class);
        } catch (RestClientResponseException e) {
            // 원인을 알 수 있게 상태 코드와 응답 앞부분을 남긴다. 응답에 키가 섞여 오더라도 지운다
            throw new PublicDataApiException("KOPIS 호출 실패(" + keyword + "): HTTP " + e.getStatusCode().value()
                    + " " + mask(e.getResponseBodyAsString()));
        } catch (RuntimeException e) {
            throw new PublicDataApiException("KOPIS 호출 실패(" + keyword + "): " + e.getClass().getSimpleName());
        }
        return parse(xml);
    }

    /**
     * 응답 본문을 로그용으로 줄인다. 웹서버 HTML 오류 페이지면 제목(title)만, 아니면 태그를 걷고 앞 200자만 남긴다.
     * 응답에 인증키가 섞여 오더라도 지운다.
     */
    String mask(String body) {
        if (body == null) return "";
        Matcher title = TITLE.matcher(body);
        String text = title.find() ? title.group(1) : body.replaceAll("<[^>]+>", " ");
        text = text.replaceAll("\s+", " ").trim();
        if (serviceKey != null && !serviceKey.isBlank()) text = text.replace(serviceKey, "***");
        return text.length() <= 200 ? text : text.substring(0, 200) + "…";
    }

    static List<Performance> parse(byte[] xml) {
        List<Performance> result = new ArrayList<>();
        if (xml == null || xml.length == 0) return result;
        Document doc;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        } catch (Exception e) {
            throw new PublicDataApiException("KOPIS 응답 해석 실패: " + e.getClass().getSimpleName());
        }
        NodeList rows = doc.getElementsByTagName("db");
        for (int i = 0; i < rows.getLength(); i++) {
            Element row = (Element) rows.item(i);
            String id = text(row, "mt20id");
            if (id == null) continue;
            result.add(new Performance(id, text(row, "prfnm"), date(text(row, "prfpdfrom")), date(text(row, "prfpdto")),
                    text(row, "poster"), text(row, "fcltynm"), text(row, "genrenm")));
        }
        return result;
    }

    private static String text(Element row, String tag) {
        NodeList nodes = row.getElementsByTagName(tag);
        if (nodes.getLength() == 0) return null;
        String value = nodes.item(0).getTextContent();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static LocalDate date(String dotted) {
        if (dotted == null) return null;
        try {
            return LocalDate.parse(dotted, DOTTED);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
