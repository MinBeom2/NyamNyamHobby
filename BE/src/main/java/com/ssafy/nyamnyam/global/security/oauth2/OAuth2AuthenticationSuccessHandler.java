package com.ssafy.nyamnyam.global.security.oauth2;

import com.ssafy.nyamnyam.domain.auth.dto.response.LoginResult;
import com.ssafy.nyamnyam.domain.auth.entity.AuthProvider;
import com.ssafy.nyamnyam.domain.auth.entity.SocialAccount;
import com.ssafy.nyamnyam.domain.auth.repository.SocialAccountRepository;
import com.ssafy.nyamnyam.domain.auth.service.AuthService;
import com.ssafy.nyamnyam.domain.user.entity.User;
import com.ssafy.nyamnyam.domain.user.repository.UserRepository;
import com.ssafy.nyamnyam.global.security.jwt.JwtProvider;
import com.ssafy.nyamnyam.global.util.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final SocialAccountRepository socialAccountRepository;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        if (!(authentication.getPrincipal() instanceof CustomOAuth2User customOAuth2User)) {
            log.warn("OAuth2 principal is not CustomOAuth2User: {}", authentication.getPrincipal());
            sendErrorRedirect(request, response, "인증 정보를 확인할 수 없습니다.");
            return;
        }

        AuthProvider provider = customOAuth2User.getProvider();
        OAuth2UserInfo userInfo = customOAuth2User.getUserInfo();
        String providerUserId = userInfo.getProviderUserId();
        String email = userInfo.getEmail();
        String name = userInfo.getName();

        log.info("[OAuth2 Success] provider={}, providerUserId={}, email={}", provider, providerUserId, email);

        // 1) 이미 연동된 소셜 계정이 있는 경우 -> 로그인 토큰 발급 후 SUCCESS 리다이렉트
        Optional<SocialAccount> socialAccountOpt = socialAccountRepository.findByProviderAndProviderUserId(provider, providerUserId);
        if (socialAccountOpt.isPresent()) {
            User user = socialAccountOpt.get().getUser();
            LoginResult loginResult = authService.issueTokens(user, LocalDateTime.now());

            CookieUtils.addRefreshTokenCookie(
                    response,
                    loginResult.getRefreshToken(),
                    jwtProvider.getRefreshTokenValidityInSeconds()
            );

            String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth/callback")
                    .queryParam("status", "SUCCESS")
                    .queryParam("accessToken", loginResult.getAccessToken())
                    .queryParam("userId", loginResult.getUser().getId())
                    .queryParam("email", loginResult.getUser().getEmail())
                    .queryParam("nickname", loginResult.getUser().getNickname())
                    .queryParam("role", loginResult.getUser().getRole())
                    .queryParam("onboarded", loginResult.getUser().isOnboarded())
                    .build()
                    .encode(java.nio.charset.StandardCharsets.UTF_8)
                    .toUriString();

            getRedirectStrategy().sendRedirect(request, response, targetUrl);
            return;
        }

        // 2) 소셜 계정은 없으나 일반 이메일로 가입된 계정이 이미 존재하는 경우 -> EMAIL_EXISTS 리다이렉트 (자동 연동 금지)
        if (email != null && userRepository.existsByEmail(email)) {
            log.info("[OAuth2 Conflict] Email already registered with normal account: email={}", email);
            String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth/callback")
                    .queryParam("status", "EMAIL_EXISTS")
                    .queryParam("email", email)
                    .queryParam("provider", provider.name())
                    .build()
                    .encode(java.nio.charset.StandardCharsets.UTF_8)
                    .toUriString();

            getRedirectStrategy().sendRedirect(request, response, targetUrl);
            return;
        }

        // 3) 신규 소셜 회원 -> 임시 가입 토큰 발급 후 SIGNUP_REQUIRED 리다이렉트
        String signupToken = jwtProvider.createOAuthSignupToken(provider.name(), providerUserId, email != null ? email : "");

        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth/callback")
                .queryParam("status", "SIGNUP_REQUIRED")
                .queryParam("signupToken", signupToken)
                .queryParam("email", email != null ? email : "")
                .queryParam("defaultNickname", name != null ? name : "")
                .build()
                .encode(java.nio.charset.StandardCharsets.UTF_8)
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private void sendErrorRedirect(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth/callback")
                .queryParam("status", "FAILED")
                .queryParam("message", message)
                .build()
                .encode(java.nio.charset.StandardCharsets.UTF_8)
                .toUriString();
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}

