package com.ssafy.nyamnyam.domain.hobby.controller;

import com.ssafy.nyamnyam.domain.hobby.dto.response.HobbyDetailResponse;
import com.ssafy.nyamnyam.domain.hobby.service.HobbyService;
import com.ssafy.nyamnyam.global.common.ApiResponse;
import com.ssafy.nyamnyam.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 취미 검색·상세 API(/api/v1/hobbies). 공개 경로라 principal은 비회원이면 null이다.
 */
@RestController
@RequestMapping("/api/v1/hobbies")
@RequiredArgsConstructor
public class HobbyController {

    private final HobbyService hobbyService;

    @GetMapping("/{hobbyId}")
    public ApiResponse<HobbyDetailResponse> detail(@PathVariable Long hobbyId,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        Long userId = principal != null ? principal.getUserId() : null;
        return ApiResponse.success(hobbyService.getDetail(hobbyId, userId));
    }
}
