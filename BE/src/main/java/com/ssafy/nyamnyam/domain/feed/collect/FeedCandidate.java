package com.ssafy.nyamnyam.domain.feed.collect;

import com.ssafy.nyamnyam.domain.feed.entity.FeedOrigin;
import com.ssafy.nyamnyam.domain.feed.entity.FeedType;

import java.time.LocalDate;

/**
 * 외부 출처에서 받은 행사 1건을 저장 전 공통 형태로 바꾼 것. 후보 취미는 저장 직전에 제목으로 정한다.
 *
 * @param sourceRef      출처 안에서의 고유 ID. (origin, sourceRef)로 중복을 막는다
 * @param regionCode     시군구 코드(regions.code). 모르면 null
 * @param sourceCategory 출처가 준 분류명(서울 예약 소분류, KOPIS 장르, 문화포털 분야). GMS 분류 단서, 모르면 null
 */
public record FeedCandidate(
        FeedOrigin origin,
        String sourceRef,
        FeedType type,
        String title,
        String summary,
        String sourceName,
        String sourceUrl,
        String thumbnailUrl,
        String regionCode,
        LocalDate eventStart,
        LocalDate eventEnd,
        String sourceCategory
) {
}
