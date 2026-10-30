package com.ssafy.nyamnyam.domain.hobby.dto.response;

import com.ssafy.nyamnyam.domain.hobby.dto.response.HobbyDetailResponse.CategoryResponse;
import com.ssafy.nyamnyam.domain.hobby.dto.response.HobbyDetailResponse.RangeResponse;
import com.ssafy.nyamnyam.domain.hobby.entity.Hobby;
import com.ssafy.nyamnyam.domain.hobby.entity.HobbyTag;
import com.ssafy.nyamnyam.domain.hobby.entity.Tag;

import java.util.Comparator;
import java.util.List;

/** 취미 검색 결과의 카드 1개(API 명세 3.4-1 content 항목). 상세 응답과 같은 하위 구조를 쓴다 */
public record HobbySummaryResponse(
        Long id,
        String name,
        CategoryResponse category,
        String summary,
        String thumbnailUrl,
        String difficulty,
        String placeType,
        String socialType,
        RangeResponse sessionMinutes,
        RangeResponse monthlyCost,
        int saveCount,
        List<String> tags
) {
    public static HobbySummaryResponse from(Hobby hobby) {
        List<String> tags = hobby.getHobbyTags().stream()
                .map(HobbyTag::getTag)
                .sorted(Comparator.comparing(Tag::getId))
                .map(Tag::getName)
                .toList();
        return new HobbySummaryResponse(
                hobby.getId(),
                hobby.getName(),
                new CategoryResponse(hobby.getCategory().getId(), hobby.getCategory().getName()),
                hobby.getSummary(),
                hobby.getThumbnailUrl(),
                hobby.getDifficulty().name(),
                hobby.getPlaceType().name(),
                hobby.getSocialType().name(),
                new RangeResponse(hobby.getSessionMinMinutes(), hobby.getSessionMaxMinutes()),
                new RangeResponse(hobby.getMonthlyCostMin(), hobby.getMonthlyCostMax()),
                hobby.getSaveCount(),
                tags
        );
    }
}
