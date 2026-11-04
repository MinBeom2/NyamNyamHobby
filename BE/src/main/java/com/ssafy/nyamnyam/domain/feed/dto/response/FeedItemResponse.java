package com.ssafy.nyamnyam.domain.feed.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.nyamnyam.domain.feed.entity.FeedItem;
import com.ssafy.nyamnyam.domain.feed.entity.FeedType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record FeedItemResponse(
        Long id,
        Long hobbyId,
        String hobbyName,
        FeedType type,
        String title,
        String summary,
        String sourceName,
        String sourceUrl,
        String thumbnailUrl,
        LocalDate eventStart,
        LocalDate eventEnd,
        OffsetDateTime publishedAt,
        @JsonProperty("isEnded") boolean isEnded
) {
    public static FeedItemResponse from(FeedItem item) {
        return new FeedItemResponse(
                item.getId(),
                item.getHobby().getId(),
                item.getHobby().getName(),
                item.getType(),
                item.getTitle(),
                item.getSummary(),
                item.getSourceName(),
                item.getSourceUrl(),
                item.getThumbnailUrl(),
                item.getEventStart(),
                item.getEventEnd(),
                toKst(item.getPublishedAt()),
                item.computeIsEnded()
        );
    }

    /** DB 시각은 UTC로 저장하므로 명세대로 +09:00으로 바꿔 내려준다 */
    private static OffsetDateTime toKst(LocalDateTime utc) {
        return utc == null ? null : utc.atOffset(ZoneOffset.UTC).withOffsetSameInstant(ZoneOffset.ofHours(9));
    }
}
