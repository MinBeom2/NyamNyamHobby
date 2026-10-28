package com.ssafy.nyamnyam.domain.auth.controller;

import com.ssafy.nyamnyam.domain.auth.dto.request.*;
import com.ssafy.nyamnyam.domain.auth.dto.response.*;
import com.ssafy.nyamnyam.domain.auth.service.AuthService;
import com.ssafy.nyamnyam.global.common.ApiResponse;
import com.ssafy.nyamnyam.global.security.jwt.JwtProvider;
import com.ssafy.nyamnyam.global.util.CookieUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;

    /**
     * 1) 이메일 회원가입 (201 Created)
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * 2) 이메일 중복 확인 (200 OK)
     */
    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<CheckEmailResponse>> checkEmail(
            @RequestParam("email") String email
    ) {
        CheckEmailResponse response = authService.checkEmail(email);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 3) 닉네임 중복 확인 (200 OK)
     */
    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<CheckNicknameResponse>> checkNickname(
            @RequestParam("nickname") String nickname
    ) {
        CheckNicknameResponse response = authService.checkNickname(nickname);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 4) 이메일 인증 확인 (200 OK, Refresh Token 쿠키 발급 + 자동 로그인)
     */
    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<LoginResponse>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request,
            HttpServletResponse response
    ) {
        LoginResult result = authService.verifyEmail(request);

        CookieUtils.addRefreshTokenCookie(
                response,
                result.getRefreshToken(),
                jwtProvider.getRefreshTokenValidityInSeconds()
        );

        LoginResponse loginResponse = new LoginResponse(
                result.getAccessToken(),
                result.getExpiresIn(),
                result.getUser()
        );
        return ResponseEntity.ok(ApiResponse.success(loginResponse));
    }

    /**
     * 5) 인증 메일 재발송 (200 OK)
     */
    @PostMapping("/email/resend")
    public ResponseEntity<ApiResponse<Void>> resendEmail(
            @Valid @RequestBody ResendEmailRequest request
    ) {
        authService.resendVerificationEmail(request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 6) 이메일 로그인 (200 OK, Access Token 바디, Refresh Token 쿠키)
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        LoginResult result = authService.login(request);

        CookieUtils.addRefreshTokenCookie(
                response,
                result.getRefreshToken(),
                jwtProvider.getRefreshTokenValidityInSeconds()
        );

        LoginResponse loginResponse = new LoginResponse(
                result.getAccessToken(),
                result.getExpiresIn(),
                result.getUser()
        );
        return ResponseEntity.ok(ApiResponse.success(loginResponse));
    }

    /**
     * 7) 토큰 재발급 (200 OK)
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(
            HttpServletRequest request
    ) {
        String refreshToken = CookieUtils.getCookie(request, CookieUtils.REFRESH_TOKEN_COOKIE_NAME)
                .map(Cookie::getValue)
                .orElse(null);

        TokenRefreshResponse response = authService.refreshAccessToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 8) 로그아웃 (204 No Content, Refresh Token 쿠키 만료)
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = CookieUtils.getCookie(request, CookieUtils.REFRESH_TOKEN_COOKIE_NAME)
                .map(Cookie::getValue)
                .orElse(null);

        authService.logout(refreshToken);
        CookieUtils.deleteRefreshTokenCookie(response);

        return ResponseEntity.noContent().build();
    }

    /**
     * 9) 비밀번호 재설정 메일 요청 (200 OK)
     */
    @PostMapping("/password/reset-request")
    public ResponseEntity<ApiResponse<MessageResponse>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request
    ) {
        MessageResponse response = authService.requestPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 10) 비밀번호 재설정 실행 (200 OK)
     */
    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<MessageResponse>> resetPassword(
            @Valid @RequestBody PasswordResetConfirmRequest request
    ) {
        MessageResponse response = authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 11) 소셜 회원가입 완료 (201 Created, Refresh Token 쿠키 발급 + 자동 로그인)
     */
    @PostMapping("/oauth/signup")
    public ResponseEntity<ApiResponse<LoginResponse>> oauthSignup(
            @Valid @RequestBody OAuthSignupRequest request,
            HttpServletResponse response
    ) {
        LoginResult result = authService.oauthSignup(request);

        CookieUtils.addRefreshTokenCookie(
                response,
                result.getRefreshToken(),
                jwtProvider.getRefreshTokenValidityInSeconds()
        );

        LoginResponse loginResponse = new LoginResponse(
                result.getAccessToken(),
                result.getExpiresIn(),
                result.getUser()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(loginResponse));
    }
}

