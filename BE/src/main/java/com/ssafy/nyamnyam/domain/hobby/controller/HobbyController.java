package com.ssafy.nyamnyam.domain.hobby.controller;

import com.ssafy.nyamnyam.domain.hobby.dto.request.HobbySearchCondition;
import com.ssafy.nyamnyam.domain.hobby.dto.response.HobbyDetailResponse;
import com.ssafy.nyamnyam.domain.hobby.dto.response.HobbySummaryResponse;
import com.ssafy.nyamnyam.domain.hobby.service.HobbyService;
import com.ssafy.nyamnyam.global.common.ApiResponse;
import com.ssafy.nyamnyam.global.common.PageResponse;
import com.ssafy.nyamnyam.global.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 취미 검색·상세 API(/api/v1/hobbies). 공개 경로라 principal은 비회원이면 null이다.
 */
@RestController
@RequestMapping("/api/v1/hobbies")
@RequiredArgsConstructor
public class HobbyController {

    private final HobbyService hobbyService;

    /** 취미 다중 필터 검색(명세 3.4-1). 값 검증은 {@link HobbySearchCondition#of}에서 한다 */
    @GetMapping
    public ApiResponse<PageResponse<HobbySummaryResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String time,
            @RequestParam(required = false) String cost,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String place,
            @RequestParam(required = false) String social,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal UserPrincipal principal) {
        HobbySearchCondition condition = HobbySearchCondition.of(
                keyword, time, cost, difficulty, place, social, sort, page, size);
        return ApiResponse.success(hobbyService.search(condition, userIdOf(principal)));
    }

    @GetMapping("/{hobbyId}")
    public ApiResponse<HobbyDetailResponse> detail(@PathVariable Long hobbyId,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(hobbyService.getDetail(hobbyId, userIdOf(principal)));
    }

    private static Long userIdOf(UserPrincipal principal) {
        return principal != null ? principal.getUserId() : null;
    }
}
