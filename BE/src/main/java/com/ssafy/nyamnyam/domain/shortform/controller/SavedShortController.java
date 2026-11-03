package com.ssafy.nyamnyam.domain.shortform.controller;

import com.ssafy.nyamnyam.domain.shortform.dto.response.SavedShortResponse;
import com.ssafy.nyamnyam.domain.shortform.dto.response.SavedShortSummaryResponse;
import com.ssafy.nyamnyam.domain.shortform.service.ShortSaveService;
import com.ssafy.nyamnyam.global.common.ApiResponse;
import com.ssafy.nyamnyam.global.common.PageResponse;
import com.ssafy.nyamnyam.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/me/saved-shorts")
@RequiredArgsConstructor
public class SavedShortController {
    private final ShortSaveService service;

    @GetMapping
    public ApiResponse<PageResponse<SavedShortResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(value = "hobbyId", required = false) Long hobbyId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "recent") String sort) {
        return ApiResponse.success(service.list(principal.getUserId(), hobbyId, page, size, sort));
    }

    @GetMapping("/summary")
    public ApiResponse<SavedShortSummaryResponse> summary(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(service.summary(principal.getUserId()));
    }
}
