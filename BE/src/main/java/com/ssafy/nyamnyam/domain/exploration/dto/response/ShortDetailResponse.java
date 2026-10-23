package com.ssafy.nyamnyam.domain.exploration.dto.response;

import com.ssafy.nyamnyam.domain.exploration.entity.ReactionType;
import com.ssafy.nyamnyam.domain.shortform.entity.Short;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ShortDetailResponse {

    private Long shortId;
    private String youtubeVideoId;
    private String title;
    private Integer duration;
    private HobbySimpleResponse hobby;
    private ReactionType myReaction;
    private Boolean isSaved;

    @Builder
    public ShortDetailResponse(Long shortId, String youtubeVideoId, String title, Integer duration,
                               HobbySimpleResponse hobby, ReactionType myReaction, Boolean isSaved) {
        this.shortId = shortId;
        this.youtubeVideoId = youtubeVideoId;
        this.title = title;
        this.duration = duration;
        this.hobby = hobby;
        this.myReaction = myReaction;
        this.isSaved = isSaved != null ? isSaved : false;
    }

    public static ShortDetailResponse of(Short s, ReactionType myReaction, Boolean isSaved) {
        HobbySimpleResponse hobbyResponse = null;
        if (s.getHobby() != null) {
            String categoryName = s.getHobby().getCategory() != null ? s.getHobby().getCategory().getName() : null;
            hobbyResponse = new HobbySimpleResponse(s.getHobby().getId(), s.getHobby().getName(), categoryName);
        }

        return ShortDetailResponse.builder()
                .shortId(s.getId())
                .youtubeVideoId(s.getYoutubeVideoId())
                .title(s.getTitle())
                .duration(s.getDurationSeconds() != null ? (int) s.getDurationSeconds() : null)
                .hobby(hobbyResponse)
                .myReaction(myReaction)
                .isSaved(isSaved)
                .build();
    }

    @Getter
    @NoArgsConstructor
    public static class HobbySimpleResponse {
        private Long id;
        private String name;
        private String category;

        public HobbySimpleResponse(Long id, String name, String category) {
            this.id = id;
            this.name = name;
            this.category = category;
        }
    }
}
