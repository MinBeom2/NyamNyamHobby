package com.ssafy.nyamnyam.domain.recommendation.dto.request;

import jakarta.validation.constraints.*;
import java.util.List;

/**
 * 추천 생성. EXPLORATION_SESSION은 sessionId, DIRECT_SELECTION은 hobbyIds(1~10개)가 필요하다.
 * 두 값의 필수 여부는 basis에 따라 달라서 컨트롤러에서 확인한다.
 */
public record CreateRecommendationRequest(@NotNull Basis basis, @Positive Long sessionId,
                                           @Size(max = 10, message = "직접 선택은 최대 10개까지 가능합니다.") List<@NotNull @Positive Long> hobbyIds) {
    public enum Basis { EXPLORATION_SESSION, DIRECT_SELECTION }
}
