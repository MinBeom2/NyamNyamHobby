package com.ssafy.nyamnyam.domain.recommendation.dto.response;

import com.ssafy.nyamnyam.domain.recommendation.model.HobbyMatch;
import java.util.List;

public record RecommendationPreviewResponse(List<Item> items) {
    public static RecommendationPreviewResponse from(List<HobbyMatch> matches) {
        return new RecommendationPreviewResponse(matches.stream().map(match -> new Item(
                match.hobbyId(), match.name(), match.summary(), match.thumbnailUrl(),
                match.similarity(), match.rank())).toList());
    }

    public record Item(long hobbyId, String name, String summary, String thumbnailUrl,
                       double similarity, int rank) { }
}
