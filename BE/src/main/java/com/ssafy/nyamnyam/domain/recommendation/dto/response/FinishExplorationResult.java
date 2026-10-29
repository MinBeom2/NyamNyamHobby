package com.ssafy.nyamnyam.domain.recommendation.dto.response;

/** 생성 여부는 HTTP 상태 결정에만 사용하며 응답 본문에는 포함하지 않는다. */
public record FinishExplorationResult(RecommendationResponse response, boolean created) {
}
