package com.ssafy.nyamnyam.domain.exploration.dto.response;

import com.ssafy.nyamnyam.domain.exploration.entity.ReactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExplorationProgressResponse {

    private Integer currentStep;
    private Integer maxCount;
    private List<ReactedHobbyResponse> reactedHobbies;
    private List<TopInterestResponse> topInterests;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReactedHobbyResponse {
        private Long id;
        private String name;
        private ReactionType reaction;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopInterestResponse {
        private String hobbyName;
        private Integer percent;
    }
}
