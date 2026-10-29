package com.ssafy.nyamnyam.domain.hobby.controller;

import com.ssafy.nyamnyam.domain.hobby.service.HobbyVideoService;
import com.ssafy.nyamnyam.global.common.ApiResponse;
import com.ssafy.nyamnyam.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/hobbies")
@RequiredArgsConstructor
public class HobbyVideoController {
    private final HobbyVideoService service;
    public record DislikeResponse(Long hobbyId, boolean disliked) {}
    @PutMapping("/{hobbyId}/dislike")
    public ApiResponse<DislikeResponse> dislike(@PathVariable Long hobbyId, @AuthenticationPrincipal UserPrincipal principal) {
        service.dislike(principal.getUserId(), hobbyId, true);
        return ApiResponse.success(new DislikeResponse(hobbyId, true));
    }
    @DeleteMapping("/{hobbyId}/dislike")
    public ApiResponse<DislikeResponse> cancel(@PathVariable Long hobbyId, @AuthenticationPrincipal UserPrincipal principal) {
        service.dislike(principal.getUserId(), hobbyId, false);
        return ApiResponse.success(new DislikeResponse(hobbyId, false));
    }
    @GetMapping("/{hobbyId}/shorts")
    public ApiResponse<List<HobbyVideoService.ShortCard>> shorts(@PathVariable Long hobbyId, @RequestParam(defaultValue = "6") int limit) {
        return ApiResponse.success(service.relatedShorts(hobbyId, limit));
    }
}
