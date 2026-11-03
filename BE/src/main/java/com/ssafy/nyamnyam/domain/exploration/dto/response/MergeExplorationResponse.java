package com.ssafy.nyamnyam.domain.exploration.dto.response;

/**
 * 비회원 탐색 기록 이관 응답 DTO
 */
public record MergeExplorationResponse(
        int mergedReactionsCount,
        int mergedSessionCount
) {
}

