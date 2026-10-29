package com.ssafy.nyamnyam.domain.recommendation.dto.response;

import com.ssafy.nyamnyam.domain.hobby.entity.Difficulty;
import com.ssafy.nyamnyam.domain.hobby.entity.PlaceType;
import com.ssafy.nyamnyam.domain.hobby.entity.SocialType;
import com.ssafy.nyamnyam.domain.recommendation.entity.ReasonStatus;
import com.ssafy.nyamnyam.domain.recommendation.entity.RecoBasis;
import com.ssafy.nyamnyam.domain.recommendation.entity.RecoItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationResponse {

    private Long recommendationId;
    private RecoBasis basis;
    private Boolean isExploratory;
    private Integer reactionCount;
    private List<RecommendationItemDto> items;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecommendationItemDto {
        private Long itemId;
        private Long hobbyId;
        private String hobbyName;
        private String category;
        private RecoItemType type;
        private Integer rankNo;
        private BigDecimal score;
        private String reason;
        private ReasonStatus reasonStatus;
        private Difficulty difficulty;
        private String sessionMinutes;
        private String monthlyCost;
        private PlaceType placeType;
        private SocialType socialType;
        private Boolean conditionWarning;
    }
}
