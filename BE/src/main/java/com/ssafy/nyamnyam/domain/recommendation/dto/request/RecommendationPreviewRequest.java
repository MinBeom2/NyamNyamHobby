package com.ssafy.nyamnyam.domain.recommendation.dto.request;

import com.ssafy.nyamnyam.domain.recommendation.model.TagFeature;
import jakarta.validation.constraints.*;
import java.util.Map;
import java.util.Set;

public record RecommendationPreviewRequest(
        @NotNull @Size(max = 24) Map<@NotNull TagFeature, @NotNull Double> preferenceScores,
        @Min(1) @Max(100) Integer limit,
        Set<@NotNull @Positive Long> excludedHobbyIds) {

    public RecommendationPreviewRequest {
        if (limit == null) limit = 3;
        if (excludedHobbyIds == null) excludedHobbyIds = Set.of();
    }
}
