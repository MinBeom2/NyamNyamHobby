package com.ssafy.nyamnyam.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 로그인 결과를 서비스 내부에서 전달하기 위한 DTO.
 * refreshToken 원본은 Controller에서 쿠키로 세팅 후 body에는 포함하지 않음.
 */
@Getter
@AllArgsConstructor
public class LoginResult {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private UserSummaryResponse user;
}
