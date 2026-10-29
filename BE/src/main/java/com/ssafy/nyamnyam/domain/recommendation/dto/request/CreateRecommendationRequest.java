package com.ssafy.nyamnyam.domain.recommendation.dto.request;

import jakarta.validation.constraints.*;
import java.util.List;

public record CreateRecommendationRequest(@NotNull Basis basis, @NotNull @Positive Long sessionId,
                                           @Size(max = 0, message = "직접 선택은 MVP에서 지원하지 않습니다.") List<Long> hobbyIds) {
    public enum Basis { EXPLORATION_SESSION, DIRECT_SELECTION }
}
