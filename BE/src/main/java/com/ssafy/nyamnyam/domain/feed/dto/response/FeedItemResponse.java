package com.ssafy.nyamnyam.domain.feed.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.nyamnyam.domain.feed.entity.FeedItem;
import com.ssafy.nyamnyam.domain.feed.entity.FeedType;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
        LocalDateTime publishedAt,
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
                item.getPublishedAt(),
                item.computeIsEnded()
        );
    }
}

