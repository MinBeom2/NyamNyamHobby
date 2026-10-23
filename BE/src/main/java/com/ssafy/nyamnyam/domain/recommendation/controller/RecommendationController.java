package com.ssafy.nyamnyam.domain.recommendation.controller;

import com.ssafy.nyamnyam.domain.recommendation.dto.request.RecommendationPreviewRequest;
import com.ssafy.nyamnyam.domain.recommendation.dto.response.RecommendationPreviewResponse;
import com.ssafy.nyamnyam.domain.recommendation.service.HobbyRecommendationService;
import com.ssafy.nyamnyam.global.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {
    private final HobbyRecommendationService service;

    public RecommendationController(HobbyRecommendationService service) {
        this.service = service;
    }

    /** 전달받은 점수로만 추천한다. 사용자 선호나 추천 결과를 저장하지 않는다. */
    @PostMapping("/preview")
    public ApiResponse<RecommendationPreviewResponse> preview(
            @Valid @RequestBody RecommendationPreviewRequest request) {
        return ApiResponse.success(RecommendationPreviewResponse.from(service.recommendByPreference(
                request.preferenceScores(), request.limit(), request.excludedHobbyIds())));
    }
}
