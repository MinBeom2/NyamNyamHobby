package com.ssafy.nyamnyam.domain.feed.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ssafy.nyamnyam.domain.feed.entity.FeedItem;
import com.ssafy.nyamnyam.domain.feed.entity.FeedType;
import com.ssafy.nyamnyam.domain.user.entity.Region;

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
        String regionName,
        LocalDate eventStart,
        LocalDate eventEnd,
        OffsetDateTime publishedAt,
        @JsonProperty("isEnded") boolean isEnded,
        /** 강좌 접수가 끝남(접수종료·예약마감 등). 행사는 진행 중이라 자세히 보기는 열어 둔다 */
        @JsonProperty("isClosed") boolean isClosed
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
                regionName(item.getRegion()),
                item.getEventStart(),
                item.getEventEnd(),
                toKst(item.getPublishedAt()),
                item.computeIsEnded(),
                item.isReservationClosed()
        );
    }

    /** 진행 지역 표기(예: '서울 송파구'). 지역이 없는 항목(입문 자료, 시군구를 모르는 공연)은 null */
    private static String regionName(Region region) {
        if (region == null) return null;
        String sido = region.getSido().replaceAll("(특별시|광역시|특별자치시)$", "");
        return sido + " " + region.getSigungu();
    }

    /** DB 시각은 UTC로 저장하므로 명세대로 +09:00으로 바꿔 내려준다 */
    private static OffsetDateTime toKst(LocalDateTime utc) {
        return utc == null ? null : utc.atOffset(ZoneOffset.UTC).withOffsetSameInstant(ZoneOffset.ofHours(9));
    }
}
