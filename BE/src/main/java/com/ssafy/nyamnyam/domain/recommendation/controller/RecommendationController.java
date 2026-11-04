package com.ssafy.nyamnyam.domain.recommendation.controller;

import com.ssafy.nyamnyam.domain.recommendation.dto.request.RecommendationPreviewRequest;
import com.ssafy.nyamnyam.domain.recommendation.dto.response.RecommendationPreviewResponse;
import com.ssafy.nyamnyam.domain.recommendation.service.HobbyRecommendationService;
import com.ssafy.nyamnyam.domain.recommendation.service.RecommendationService;
import com.ssafy.nyamnyam.domain.recommendation.dto.request.CreateRecommendationRequest;
import com.ssafy.nyamnyam.domain.recommendation.dto.response.RecommendationResponse;
import com.ssafy.nyamnyam.domain.user.entity.User;
import com.ssafy.nyamnyam.domain.user.repository.UserRepository;
import com.ssafy.nyamnyam.global.error.*;
import com.ssafy.nyamnyam.global.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import com.ssafy.nyamnyam.global.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {
    private final HobbyRecommendationService service;
    private final RecommendationService recommendations;
    private final UserRepository users;

    @PostMapping
    public ResponseEntity<ApiResponse<RecommendationResponse>> create(@Valid @RequestBody CreateRecommendationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (request.basis() != CreateRecommendationRequest.Basis.EXPLORATION_SESSION)
            throw new CustomException(ErrorCode.COMMON_INVALID_INPUT, "직접 선택 기반 추천은 MVP에서 지원하지 않습니다.");
        var result = recommendations.finishExploration(request.sessionId(), user(principal));
        return ResponseEntity.status(result.created() ? 201 : 200).body(ApiResponse.success(result.response()));
    }
    /** 홈 '맞춤 추천 취미'용 내 최근 추천. 없으면 data가 null이다. */
    @GetMapping("/latest")
    public ApiResponse<RecommendationResponse> latest(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(recommendations.getLatestRecommendation(user(principal)).orElse(null));
    }
    @GetMapping("/{id}")
    public ApiResponse<RecommendationResponse> detail(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(recommendations.getRecommendation(id, user(principal)));
    }
    private User user(UserPrincipal principal) {
        if (principal == null) throw new CustomException(ErrorCode.COMMON_UNAUTHORIZED);
        return users.findById(principal.getUserId()).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    /** 전달받은 점수로만 추천한다. 사용자 선호나 추천 결과를 저장하지 않는다. */
    @PostMapping("/preview")
    public ApiResponse<RecommendationPreviewResponse> preview(
            @Valid @RequestBody RecommendationPreviewRequest request) {
        return ApiResponse.success(RecommendationPreviewResponse.from(service.recommendByPreference(
                request.preferenceScores(), request.limit(), request.excludedHobbyIds())));
    }
}
