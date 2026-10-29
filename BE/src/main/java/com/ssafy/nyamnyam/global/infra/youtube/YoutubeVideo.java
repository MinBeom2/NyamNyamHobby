package com.ssafy.nyamnyam.global.infra.youtube;

import java.time.LocalDateTime;
import java.util.List;

/**
 * videos.list 응답 한 건을 수집에 필요한 값만 남겨 정리한 것.
 *
 * @param durationSeconds 길이(초). 라이브·예정 영상처럼 길이를 알 수 없으면 -1
 * @param allowedRegions  재생 허용 국가 목록. 제한이 없으면 null
 * @param blockedRegions  재생 차단 국가 목록. 제한이 없으면 null
 */
public record YoutubeVideo(
        String videoId,
        String channelId,
        String channelTitle,
        String title,
        String description,
        String thumbnailUrl,
        int durationSeconds,
        LocalDateTime publishedAt,
        String privacyStatus,
        boolean embeddable,
        List<String> allowedRegions,
        List<String> blockedRegions,
        long viewCount
) {
}
