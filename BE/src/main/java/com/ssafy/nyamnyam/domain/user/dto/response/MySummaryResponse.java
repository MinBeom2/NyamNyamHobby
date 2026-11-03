package com.ssafy.nyamnyam.domain.user.dto.response;

/**
 * 마이페이지 요약 수치 응답 (GET /api/v1/users/me/summary)
 */
public record MySummaryResponse(
        long savedHobbyCount,
        long inProgressHobbyCount,
        long savedShortCount,
        long myPostCount
) {
}

