package com.ssafy.nyamnyam.domain.recommendation.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationReasonEvent implements Serializable {

    private Long recommendationId;
    private Long userId;
    private List<ItemReasonPayload> items;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemReasonPayload implements Serializable {
        private Long itemId;
        private Long hobbyId;
        private String hobbyName;
        private String subjectTag;
        private String gainTag;
        private String userReactionSummary;
    }
}
