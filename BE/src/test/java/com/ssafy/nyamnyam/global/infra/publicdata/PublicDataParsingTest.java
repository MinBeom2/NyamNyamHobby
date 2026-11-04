package com.ssafy.nyamnyam.global.infra.publicdata;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PublicDataParsingTest {

    @Test void tourHomepageKeepsOnlyTheLink() {
        assertThat(TourApiClient.homepage("<a href=\"https://fest.example.com\" target=\"_blank\">홈페이지</a>"))
                .isEqualTo("https://fest.example.com");
        assertThat(TourApiClient.homepage("www.example.com")).isEqualTo("https://www.example.com");
        assertThat(TourApiClient.homepage("없음")).isNull();
        assertThat(TourApiClient.stripTags("첫 줄<br>둘째 &amp; 셋째")).isEqualTo("첫 줄 둘째 & 셋째");
    }

    @Test void kopisXmlIsParsed() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><dbs><db><mt20id>PF1</mt20id><prfnm>기타 리사이틀</prfnm>"
                + "<prfpdfrom>2026.11.25</prfpdfrom><prfpdto>2026.11.26</prfpdto><fcltynm>홀</fcltynm>"
                + "<poster>http://p.jpg</poster><genrenm>대중음악</genrenm></db></dbs>";
        var shows = KopisClient.parse(xml.getBytes(StandardCharsets.UTF_8));
        assertThat(shows).singleElement().satisfies(show -> {
            assertThat(show.id()).isEqualTo("PF1");
            assertThat(show.start()).isEqualTo(LocalDate.of(2026, 11, 25));
            assertThat(show.end()).isEqualTo(LocalDate.of(2026, 11, 26));
            assertThat(show.genre()).isEqualTo("대중음악");
        });
    }

    @Test void cultureXmlItemsAndErrorsAreRead() {
        String ok = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><response><header><resultCode>00</resultCode></header>"
                + "<body><totalCount>1</totalCount><items><item><seq>7</seq><title>기획전</title><realmName>전시</realmName>"
                + "</item></items></body></response>";
        var doc = CulturePortalClient.parse(ok.getBytes(StandardCharsets.UTF_8));
        assertThat(CulturePortalClient.items(doc)).singleElement()
                .satisfies(item -> assertThat(CulturePortalClient.text(item, "realmName")).isEqualTo("전시"));
    }

    @Test void kopisErrorPageKeepsOnlyTitleAndHidesKey() {
        KopisClient client = new KopisClient("http://localhost", "secret-key");
        String html = "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\"><html><head><title>400 Bad Request</title></head>"
                + "<body><p>bad request secret-key</p></body></html>";
        assertThat(client.mask(html)).isEqualTo("400 Bad Request");
        assertThat(client.mask("<error>key secret-key invalid</error>")).isEqualTo("key *** invalid");
    }

    @Test void seoulTitleEntitiesAndDatesAreCleaned() {
        assertThat(SeoulReservationClient.unescape("2026년 하반기 &#39;우리 가족 경희궁 탐험대&#39; &amp; 체험"))
                .isEqualTo("2026년 하반기 '우리 가족 경희궁 탐험대' & 체험");
        assertThat(SeoulReservationClient.date("2026-11-21 00:00:00.0")).isEqualTo(LocalDate.of(2026, 11, 21));
        assertThat(SeoulReservationClient.date(null)).isNull();
    }

    @Test void doubleEscapedAndNamedEntitiesBecomeText() {
        // 문화포털: XML에 '&amp;lt;'로 와서 한 번 풀어도 '&lt;'가 남는다
        assertThat(HtmlText.unescape("김환, 남종현 &lt;마주하는, 마주하지 않는 Facing, Without Facing&gt;"))
                .isEqualTo("김환, 남종현 <마주하는, 마주하지 않는 Facing, Without Facing>");
        assertThat(HtmlText.unescape("시니어와 함께하는 &amp;lt;미술관 한걸음&amp;gt;")).isEqualTo("시니어와 함께하는 <미술관 한걸음>");
        assertThat(HtmlText.unescape("달리&middot;Differently&middot;Dali")).isEqualTo("달리·Differently·Dali");
        assertThat(HtmlText.unescape("&#39;경희궁&#39; &#x27;탐험대&#x27;")).isEqualTo("'경희궁' '탐험대'");
        assertThat(HtmlText.unescape("모르는 &unknown; 참조와 R&D")).isEqualTo("모르는 &unknown; 참조와 R&D");
        assertThat(HtmlText.unescape("  ")).isNull();
    }

    @Test void plainStripsTagsForDescriptions() {
        assertThat(HtmlText.plain("<p>첫 줄<br/>둘째&nbsp;줄</p>  <b>끝</b>")).isEqualTo("첫 줄 둘째 줄 끝");
        assertThat(HtmlText.plain("<p></p>")).isNull();
    }
}
