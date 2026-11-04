package com.ssafy.nyamnyam.domain.feed.controller;

import com.ssafy.nyamnyam.domain.feed.dto.response.FeedItemResponse;
import com.ssafy.nyamnyam.domain.feed.entity.FeedScope;
import com.ssafy.nyamnyam.domain.feed.entity.FeedType;
import com.ssafy.nyamnyam.domain.feed.service.FeedService;
import com.ssafy.nyamnyam.global.common.ApiResponse;
import com.ssafy.nyamnyam.global.common.PageResponse;
import com.ssafy.nyamnyam.global.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feeds")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    /**
     * 취미 정보 피드 목록 조회 [공개]. 회원은 기본으로 내 취미(MY) 기준, scope로 SAVED·ALL 전환
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<FeedItemResponse>>> getFeeds(
            @RequestParam(value = "scope", required = false) FeedScope scope,
            @RequestParam(value = "hobbyId", required = false) Long hobbyId,
            @RequestParam(value = "type", required = false) FeedType type,
            @RequestParam(value = "excludeEnded", defaultValue = "true") Boolean excludeEnded,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = principal == null ? null : principal.getUserId();
        PageResponse<FeedItemResponse> response = feedService.getFeeds(userId, scope, hobbyId, type, excludeEnded, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

