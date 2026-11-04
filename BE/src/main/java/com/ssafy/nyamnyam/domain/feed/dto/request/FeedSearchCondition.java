package com.ssafy.nyamnyam.domain.feed.dto.request;

import com.ssafy.nyamnyam.domain.feed.entity.FeedScope;
import com.ssafy.nyamnyam.domain.feed.entity.FeedType;

import java.time.LocalDate;
import java.util.List;

/**
 * 정보 피드 목록 조건.
 *
 * @param scope        취미 범위(회원 기본 MY, 비회원은 항상 ALL)
 * @param hobbyId      범위 안에서 한 취미로 좁힐 때
 * @param excludeEnded 마감·종료 숨김(기본 true): 기간이 끝난 행사와 접수가 끝난 강좌를 뺀다
 * @param keyword      제목·요약 검색어. 2자 미만이면 무시
 * @param from         진행 기간이 [from, to]와 겹치는 항목만. 기간 조건이 있으면 기간이 없는 항목(입문 자료)은 빠진다
 * @param to           위 기간의 끝(포함). from만 있으면 그날 이후, to만 있으면 그날까지
 * @param regionIds    이 시군구에서 열리는 항목만. 지역 조건이 있으면 지역이 없는 항목은 빠진다
 */
public record FeedSearchCondition(
        FeedScope scope,
        Long hobbyId,
        FeedType type,
        boolean excludeEnded,
        String keyword,
        LocalDate from,
        LocalDate to,
        List<Long> regionIds
) {
    public static final int MIN_KEYWORD = 2;

    public FeedSearchCondition {
        keyword = keyword == null || keyword.trim().length() < MIN_KEYWORD ? null : keyword.trim();
        regionIds = regionIds == null ? List.of() : List.copyOf(regionIds);
    }

    public boolean hasPeriod() {
        return from != null || to != null;
    }
}
