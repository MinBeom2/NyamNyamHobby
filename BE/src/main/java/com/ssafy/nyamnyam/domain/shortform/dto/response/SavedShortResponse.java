package com.ssafy.nyamnyam.domain.shortform.dto.response;

import com.ssafy.nyamnyam.domain.shortform.entity.ShortStatus;
import java.time.OffsetDateTime;

public record SavedShortResponse(Long shortId, String youtubeVideoId, String title,
                                 String thumbnailUrl, HobbyInfo hobby, ShortStatus status,
                                 OffsetDateTime savedAt) {
    public record HobbyInfo(Long id, String name) {}
}
