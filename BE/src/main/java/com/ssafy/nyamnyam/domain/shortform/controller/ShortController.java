package com.ssafy.nyamnyam.domain.shortform.controller;

import com.ssafy.nyamnyam.domain.exploration.dto.request.ReactionRequest;
import com.ssafy.nyamnyam.domain.exploration.dto.response.ReactionResponse;
import com.ssafy.nyamnyam.domain.exploration.service.ExplorationService;
import com.ssafy.nyamnyam.domain.shortform.service.ShortSaveService;
import com.ssafy.nyamnyam.domain.shortform.service.ShortFeedService;
import com.ssafy.nyamnyam.domain.shortform.dto.response.ShortFeedResponse;
import com.ssafy.nyamnyam.domain.shortform.dto.response.ShortSaveResponse;
import com.ssafy.nyamnyam.domain.user.entity.User;
import com.ssafy.nyamnyam.domain.user.repository.UserRepository;
import com.ssafy.nyamnyam.global.common.ApiResponse;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import com.ssafy.nyamnyam.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shorts")
@RequiredArgsConstructor
public class ShortController {

    private final ExplorationService explorationService;
    private final UserRepository userRepository;
    private final ShortSaveService shortSaveService;
    private final ShortFeedService shortFeedService;

    /**
     * 자유 탐색(상단 '숏폼 탐색'). 공개 숏폼 전체를 seed로 섞어 cursor부터 size개. 비회원도 볼 수 있다.
     * 탐색 세션·시청·반응을 기록하지 않아 맞춤 추천에 영향이 없다.
     */
    @GetMapping("/feed")
    public ApiResponse<ShortFeedResponse> feed(
            @RequestParam(value = "seed", required = false) Long seed,
            @RequestParam(value = "cursor", defaultValue = "0") int cursor,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(shortFeedService.feed(principal == null ? null : principal.getUserId(), seed, cursor, size));
    }

    @PutMapping("/{shortId}/save")
    public ApiResponse<ShortSaveResponse> save(
            @PathVariable("shortId") Long shortId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(shortSaveService.save(getUser(principal).getId(), shortId));
    }

    @DeleteMapping("/{shortId}/save")
    public ApiResponse<ShortSaveResponse> unsave(
            @PathVariable("shortId") Long shortId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(shortSaveService.unsave(getUser(principal).getId(), shortId));
    }

    /**
     * 1) 숏폼 반응 등록 및 변경 (200 OK)
     */
    @PutMapping("/{shortId}/reaction")
    public ResponseEntity<ApiResponse<ReactionResponse>> recordReaction(
            @PathVariable("shortId") Long shortId,
            @Valid @RequestBody ReactionRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = getUser(principal);
        ReactionResponse response = explorationService.recordReaction(shortId, request, user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 2) 숏폼 반응 취소 (200 OK)
     */
    @DeleteMapping("/{shortId}/reaction")
    public ResponseEntity<ApiResponse<ReactionResponse>> cancelReaction(
            @PathVariable("shortId") Long shortId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User user = getUser(principal);
        ReactionResponse response = explorationService.cancelReaction(shortId, user);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private User getUser(UserPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.COMMON_UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
