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
}
