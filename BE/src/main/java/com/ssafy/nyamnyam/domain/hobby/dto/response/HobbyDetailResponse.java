package com.ssafy.nyamnyam.domain.hobby.dto.response;

import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbyTag;
import com.ssafy.nyamnyam.domain.hobby.entity.Tag;

import java.util.Comparator;
import java.util.List;

/**
 * 취미 상세 조회 응답(API 명세 3.4-2). 필드 이름·순서는 명세의 JSON과 같다.
 * myStatus는 로그인한 사용자의 user_hobbies 상태이고, 비회원이거나 기록이 없으면 "NONE".
 */
public record HobbyDetailResponse(
        Long id,
        String name,
        CategoryResponse category,
        String summary,
        String description,
        String thumbnailUrl,
        String placeType,
        String socialType,
        String difficulty,
        RangeResponse sessionMinutes,
        RangeResponse initialCost,
        RangeResponse monthlyCost,
        String supplies,
        String environment,
        String firstActivity,
        String cautions,
        List<String> tags,
        int saveCount,
        String myStatus
) {
    public static final String STATUS_NONE = "NONE";

    public record CategoryResponse(Long id, String name) {
    }

    public record RangeResponse(int min, int max) {
    }

    public static HobbyDetailResponse of(Hobby hobby, String myStatus) {
        List<String> tags = hobby.getHobbyTags().stream()
                .map(HobbyTag::getTag)
                .sorted(Comparator.comparing(Tag::getId))
                .map(Tag::getName)
                .toList();
        return new HobbyDetailResponse(
                hobby.getId(),
                hobby.getName(),
                new CategoryResponse(hobby.getCategory().getId(), hobby.getCategory().getName()),
                hobby.getSummary(),
                hobby.getDescription(),
                hobby.getThumbnailUrl(),
                hobby.getPlaceType().name(),
                hobby.getSocialType().name(),
                hobby.getDifficulty().name(),
                new RangeResponse(hobby.getSessionMinMinutes(), hobby.getSessionMaxMinutes()),
                new RangeResponse(hobby.getInitialCostMin(), hobby.getInitialCostMax()),
                new RangeResponse(hobby.getMonthlyCostMin(), hobby.getMonthlyCostMax()),
                hobby.getSupplies(),
                hobby.getEnvironment(),
                hobby.getFirstActivity(),
                hobby.getCautions(),
                tags,
                hobby.getSaveCount(),
                myStatus != null ? myStatus : STATUS_NONE
        );
    }
}
