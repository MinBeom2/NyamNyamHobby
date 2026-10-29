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
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationResponse {

    private Long recommendationId;
    private String basis;
    private String basisText;
    private Boolean isExploratory;
    private Integer reactionCount;
    private List<RecommendationItemDto> items;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecommendationItemDto {
        private Long itemId;
        private RecoItemType type;
        private Integer rank;
        private HobbySnapshot hobby;
        private String reason;
        private ReasonStatus reasonStatus;
        private String fallbackReason;
        private Boolean conditionWarning;
        private Boolean saved;

        @JsonIgnore public Long getHobbyId() { return hobby == null ? null : hobby.id(); }
        @JsonIgnore public String getHobbyName() { return hobby == null ? null : hobby.name(); }
        @JsonIgnore public Integer getRankNo() { return rank; }
    }
    public record Range(Integer min, Integer max) {}
    public record HobbySnapshot(Long id, String name, String thumbnailUrl, Difficulty difficulty,
                                Range sessionMinutes, Range monthlyCost) {
        public static HobbySnapshot from(com.ssafy.nyamnyam.domain.hobby.entity.Hobby hobby) {
            return new HobbySnapshot(hobby.getId(), hobby.getName(), hobby.getThumbnailUrl(), hobby.getDifficulty(),
                    new Range((int) hobby.getSessionMinMinutes(), (int) hobby.getSessionMaxMinutes()),
                    new Range(hobby.getMonthlyCostMin(), hobby.getMonthlyCostMax()));
        }
    }
}
