package com.ssafy.nyamnyam.global.infra.youtube;

import com.ssafy.nyamnyam.global.infra.youtube.dto.YoutubeVideoListResponse;
import com.ssafy.nyamnyam.global.infra.youtube.dto.YoutubeVideoListResponse.ContentDetails;
import com.ssafy.nyamnyam.global.infra.youtube.dto.YoutubeVideoListResponse.Snippet;
import com.ssafy.nyamnyam.global.infra.youtube.dto.YoutubeVideoListResponse.Statistics;
import com.ssafy.nyamnyam.global.infra.youtube.dto.YoutubeVideoListResponse.Status;
import com.ssafy.nyamnyam.global.infra.youtube.dto.YoutubeVideoListResponse.Thumbnail;
import com.ssafy.nyamnyam.global.infra.youtube.dto.YoutubeVideoListResponse.Thumbnails;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/** videos.list 응답 → {@link YoutubeVideo}. 값이 비어 있어도 예외 없이 안전한 기본값을 쓴다. */
public final class YoutubeVideoMapper {

    private YoutubeVideoMapper() {
    }

    public static YoutubeVideo toVideo(YoutubeVideoListResponse.Item item) {
        Snippet snippet = item.snippet();
        ContentDetails details = item.contentDetails();
        Status status = item.status();
        Statistics statistics = item.statistics();

        return new YoutubeVideo(
                item.id(),
                snippet != null ? snippet.channelId() : null,
                snippet != null ? snippet.channelTitle() : null,
                snippet != null ? snippet.title() : null,
                snippet != null ? snippet.description() : null,
                snippet != null ? pickThumbnail(snippet.thumbnails()) : null,
                details != null ? parseDurationSeconds(details.duration()) : -1,
                snippet != null ? parsePublishedAt(snippet.publishedAt()) : null,
                status != null ? status.privacyStatus() : null,
                status != null && Boolean.TRUE.equals(status.embeddable()),
                details != null && details.regionRestriction() != null ? details.regionRestriction().allowed() : null,
                details != null && details.regionRestriction() != null ? details.regionRestriction().blocked() : null,
                statistics != null ? parseLong(statistics.viewCount()) : 0L
        );
    }

    /** ISO 8601 기간(PT1M5S 등) → 초. 비었거나 읽을 수 없거나 0이면 -1(라이브·예정 영상). */
    public static int parseDurationSeconds(String isoDuration) {
        if (isoDuration == null || isoDuration.isBlank()) {
            return -1;
        }
        try {
            long seconds = Duration.parse(isoDuration).getSeconds();
            if (seconds <= 0) {
                return -1;
            }
            return (int) Math.min(seconds, Integer.MAX_VALUE);
        } catch (DateTimeParseException e) {
            return -1;
        }
    }

    /** "2026-10-20T10:00:00Z" → JVM 시간대의 LocalDateTime(서버는 UTC). */
    public static LocalDateTime parsePublishedAt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.ofInstant(OffsetDateTime.parse(value).toInstant(), ZoneId.systemDefault());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    static long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static String pickThumbnail(Thumbnails thumbnails) {
        if (thumbnails == null) {
            return null;
        }
        for (Thumbnail candidate : new Thumbnail[]{thumbnails.high(), thumbnails.medium(), thumbnails.standardDefault()}) {
            if (candidate != null && candidate.url() != null && !candidate.url().isBlank()) {
                return candidate.url();
            }
        }
        return null;
    }
}
