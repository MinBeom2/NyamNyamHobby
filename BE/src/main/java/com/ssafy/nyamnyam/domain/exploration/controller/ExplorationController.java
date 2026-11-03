package com.ssafy.nyamnyam.domain.exploration.controller;

import com.ssafy.nyamnyam.domain.exploration.dto.request.RecordViewRequest;
import com.ssafy.nyamnyam.domain.exploration.dto.response.ExplorationProgressResponse;
import com.ssafy.nyamnyam.domain.exploration.dto.response.MergeExplorationResponse;
import com.ssafy.nyamnyam.domain.exploration.dto.response.NextShortResponse;
import com.ssafy.nyamnyam.domain.exploration.dto.response.StartExplorationResponse;
import com.ssafy.nyamnyam.domain.exploration.service.ExplorationService;
import com.ssafy.nyamnyam.domain.recommendation.dto.response.FinishExplorationResult;
import com.ssafy.nyamnyam.domain.recommendation.dto.response.RecommendationResponse;
import com.ssafy.nyamnyam.domain.recommendation.service.RecommendationService;
import com.ssafy.nyamnyam.domain.user.entity.User;
import com.ssafy.nyamnyam.domain.user.repository.UserRepository;
import com.ssafy.nyamnyam.global.common.ApiResponse;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import com.ssafy.nyamnyam.global.security.UserPrincipal;
import com.ssafy.nyamnyam.global.util.CookieUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/explorations")
@RequiredArgsConstructor
public class ExplorationController {

    private final ExplorationService explorationService;
    private final RecommendationService recommendationService;
    private final UserRepository userRepository;

    /**
     * 1) 숏폼 탐색 세션 시작 (201 Created)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<StartExplorationResponse>> startExploration(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = getUser(principal);
        StartExplorationResponse response = explorationService.startSession(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * 2) 다음 숏폼 영상 조회 (200 OK)
     */
    @GetMapping("/{sessionId}/next")
    public ResponseEntity<ApiResponse<NextShortResponse>> getNextShort(
            @PathVariable("sessionId") Long sessionId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = getUser(principal);
        NextShortResponse response = explorationService.getNextShort(sessionId, user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 3) 영상 노출/시청 시간/재생 실패 기록 (204 No Content)
     */
    @PostMapping("/{sessionId}/views")
    public ResponseEntity<Void> recordView(
            @PathVariable("sessionId") Long sessionId,
            @Valid @RequestBody RecordViewRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = getUser(principal);
        explorationService.recordView(sessionId, request, user);
        return ResponseEntity.noContent().build();
    }

    /**
     * 4) 탐색 세션 진행 현황 및 관심 주제 % 막대 조회 (200 OK)
     */
    @GetMapping("/{sessionId}/progress")
    public ResponseEntity<ApiResponse<ExplorationProgressResponse>> getProgress(
            @PathVariable("sessionId") Long sessionId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = getUser(principal);
        ExplorationProgressResponse response = explorationService.getProgress(sessionId, user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 5) 탐색 종료 및 추천 생성 (최초 201 Created, 재요청 200 OK)
     */
    @PostMapping("/{sessionId}/finish")
    public ResponseEntity<ApiResponse<RecommendationResponse>> finishExploration(
            @PathVariable("sessionId") Long sessionId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = getUser(principal);
        FinishExplorationResult result = recommendationService.finishExploration(sessionId, user);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(ApiResponse.success(result.response()));
    }

    /**
     * 6) 비회원 탐색 기록 이관 (200 OK) — FR-AUTH-05 / AUTH-B06
     * guest_key 쿠키의 세션·반응·추천을 로그인 회원에게 1회 이관하고 쿠키를 만료시킨다.
     */
    @PostMapping("/merge")
    public ResponseEntity<ApiResponse<MergeExplorationResponse>> mergeGuestExploration(
            @CookieValue(value = CookieUtils.GUEST_KEY_COOKIE_NAME, required = false) String guestKey,
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletResponse response
    ) {
        User user = getUser(principal);
        MergeExplorationResponse result = explorationService.mergeGuestData(guestKey, user);
        CookieUtils.deleteGuestKeyCookie(response);
        return ResponseEntity.ok(ApiResponse.success(result));
    }


    private User getUser(UserPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.COMMON_UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
