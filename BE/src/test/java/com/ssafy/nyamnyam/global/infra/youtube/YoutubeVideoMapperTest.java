package com.ssafy.nyamnyam.global.infra.youtube;

import com.ssafy.nyamnyam.global.infra.youtube.dto.YoutubeVideoListResponse.ContentDetails;
import com.ssafy.nyamnyam.global.infra.youtube.dto.YoutubeVideoListResponse.Item;
import com.ssafy.nyamnyam.global.infra.youtube.dto.YoutubeVideoListResponse.RegionRestriction;
import com.ssafy.nyamnyam.global.infra.youtube.dto.YoutubeVideoListResponse.Snippet;
import com.ssafy.nyamnyam.global.infra.youtube.dto.YoutubeVideoListResponse.Statistics;
import com.ssafy.nyamnyam.global.infra.youtube.dto.YoutubeVideoListResponse.Status;
import com.ssafy.nyamnyam.global.infra.youtube.dto.YoutubeVideoListResponse.Thumbnail;
import com.ssafy.nyamnyam.global.infra.youtube.dto.YoutubeVideoListResponse.Thumbnails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class YoutubeVideoMapperTest {

    @Test
    @DisplayName("ISO 8601 길이를 초로 바꾸고, 라이브·예정(0초)·잘못된 값은 -1")
    void parseDuration() {
        assertThat(YoutubeVideoMapper.parseDurationSeconds("PT58S")).isEqualTo(58);
        assertThat(YoutubeVideoMapper.parseDurationSeconds("PT1M5S")).isEqualTo(65);
        assertThat(YoutubeVideoMapper.parseDurationSeconds("PT3M")).isEqualTo(180);
        assertThat(YoutubeVideoMapper.parseDurationSeconds("PT1H2M3S")).isEqualTo(3723);
        assertThat(YoutubeVideoMapper.parseDurationSeconds("P0D")).isEqualTo(-1);
        assertThat(YoutubeVideoMapper.parseDurationSeconds("PT0S")).isEqualTo(-1);
        assertThat(YoutubeVideoMapper.parseDurationSeconds("P1W")).isEqualTo(-1);
        assertThat(YoutubeVideoMapper.parseDurationSeconds(null)).isEqualTo(-1);
    }

    @Test
    @DisplayName("응답 한 건을 수집용 값으로 변환")
    void toVideo() {
        Item item = new Item("vid1",
                new Snippet("2026-10-20T10:00:00Z", "UC123", "클라이밍TV", "볼더링 첫 도전", "설명",
                        new Thumbnails(null, new Thumbnail("https://medium.jpg"), new Thumbnail("https://default.jpg"))),
                new ContentDetails("PT45S", new RegionRestriction(null, List.of("CN"))),
                new Status("public", true),
                new Statistics("12345"));

        YoutubeVideo video = YoutubeVideoMapper.toVideo(item);

        assertThat(video.videoId()).isEqualTo("vid1");
        assertThat(video.channelId()).isEqualTo("UC123");
        assertThat(video.thumbnailUrl()).isEqualTo("https://medium.jpg");
        assertThat(video.durationSeconds()).isEqualTo(45);
        assertThat(video.embeddable()).isTrue();
        assertThat(video.blockedRegions()).containsExactly("CN");
        assertThat(video.allowedRegions()).isNull();
        assertThat(video.viewCount()).isEqualTo(12345L);
        LocalDateTime expected = LocalDateTime.of(2026, 10, 20, 10, 0)
                .atOffset(ZoneOffset.UTC).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        assertThat(video.publishedAt()).isEqualTo(expected);
    }

    @Test
    @DisplayName("값이 비어 있어도 예외 없이 기본값")
    void emptyFields() {
        YoutubeVideo video = YoutubeVideoMapper.toVideo(new Item("vid2", null, null, new Status("public", null), null));

        assertThat(video.durationSeconds()).isEqualTo(-1);
        assertThat(video.embeddable()).isFalse();
        assertThat(video.viewCount()).isZero();
        assertThat(video.publishedAt()).isNull();
    }

    @Test
    @DisplayName("오류 응답에서 reason을 꺼낸다(할당량 초과 판별용)")
    void extractReason() {
        String body = """
                {"error":{"code":403,"message":"quota","errors":[{"domain":"youtube.quota","reason":"quotaExceeded"}]}}""";
        assertThat(YoutubeClient.extractReason(body)).isEqualTo("quotaExceeded");
        assertThat(YoutubeClient.extractReason("not json")).isEqualTo("unknown");
    }

    @Test
    @DisplayName("videos.list 호출 수 = 50개 단위 올림")
    void videosCost() {
        assertThat(YoutubeClient.videosCost(0)).isZero();
        assertThat(YoutubeClient.videosCost(1)).isEqualTo(1);
        assertThat(YoutubeClient.videosCost(50)).isEqualTo(1);
        assertThat(YoutubeClient.videosCost(51)).isEqualTo(2);
    }
}
