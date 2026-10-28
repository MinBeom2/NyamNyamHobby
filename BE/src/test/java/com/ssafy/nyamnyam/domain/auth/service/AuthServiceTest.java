package com.ssafy.nyamnyam.domain.auth.service;

import com.ssafy.nyamnyam.domain.auth.dto.request.LoginRequest;
import com.ssafy.nyamnyam.domain.auth.dto.request.SignupRequest;
import com.ssafy.nyamnyam.domain.auth.dto.request.VerifyEmailRequest;
import com.ssafy.nyamnyam.domain.auth.dto.response.CheckEmailResponse;
import com.ssafy.nyamnyam.domain.auth.dto.response.LoginResult;
import com.ssafy.nyamnyam.domain.auth.dto.response.SignupResponse;
import com.ssafy.nyamnyam.domain.auth.entity.AuthToken;
import com.ssafy.nyamnyam.domain.auth.entity.TokenType;
import com.ssafy.nyamnyam.domain.auth.repository.AuthTokenRepository;
import com.ssafy.nyamnyam.domain.auth.repository.SocialAccountRepository;
import com.ssafy.nyamnyam.domain.user.entity.Role;
import com.ssafy.nyamnyam.domain.user.entity.User;
import com.ssafy.nyamnyam.domain.user.entity.UserStatus;
import com.ssafy.nyamnyam.domain.user.repository.UserConditionRepository;
import com.ssafy.nyamnyam.domain.user.repository.UserRepository;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import com.ssafy.nyamnyam.global.mail.EmailService;
import com.ssafy.nyamnyam.global.security.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserConditionRepository userConditionRepository;

    @Mock
    private AuthTokenRepository authTokenRepository;

    @Mock
    private SocialAccountRepository socialAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("회원가입 성공 - PENDING 상태 유저 생성 및 인증 메일 발송")
    void signup_Success() {
        // given
        SignupRequest request = new SignupRequest("test@example.com", "Password123!", "테스터");
        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.empty());
        given(userRepository.existsByNickname(request.getNickname())).willReturn(false);
        given(passwordEncoder.encode(request.getPassword())).willReturn("encodedPassword");

        // when
        SignupResponse response = authService.signup(request);

        // then
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getNickname()).isEqualTo("테스터");
        assertThat(response.getStatus()).isEqualTo(UserStatus.PENDING.name());
        verify(userRepository).save(any(User.class));
        verify(authTokenRepository).save(any(AuthToken.class));
        verify(emailService).sendVerificationEmail(eq("test@example.com"), any());
    }

    @Test
    @DisplayName("회원가입 실패 - 이미 존재하는 이메일")
    void signup_DuplicateEmail() {
        // given
        SignupRequest request = new SignupRequest("test@example.com", "Password123!", "테스터");
        User existingUser = User.builder()
                .email("test@example.com")
                .passwordHash("hash")
                .nickname("기존유저")
                .build();
        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.of(existingUser));

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_EMAIL_DUPLICATED);
    }

    @Test
    @DisplayName("이메일 중복 확인 - 사용 가능한 경우 available true")
    void checkEmail_Available() {
        // given
        given(userRepository.existsByEmail("available@example.com")).willReturn(false);

        // when
        CheckEmailResponse response = authService.checkEmail("available@example.com");

        // then
        assertThat(response.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("이메일 인증 성공 - ACTIVE 전환 및 즉시 로그인 토큰 발급")
    void verifyEmail_Success() {
        // given
        String token = "raw-token-1234";
        String tokenHash = JwtProvider.hashToken(token);
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("hash")
                .nickname("테스터")
                .status(UserStatus.PENDING)
                .build();

        AuthToken authToken = AuthToken.builder()
                .user(user)
                .type(TokenType.EMAIL_VERIFY)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        given(authTokenRepository.findByTokenHashAndType(tokenHash, TokenType.EMAIL_VERIFY))
                .willReturn(Optional.of(authToken));
        given(jwtProvider.createAccessToken(any(), any(), any())).willReturn("mock-access-token");
        given(jwtProvider.createRefreshToken(any())).willReturn("mock-refresh-token");
        given(jwtProvider.getAccessTokenValidityInSeconds()).willReturn(1800L);
        given(jwtProvider.getRefreshTokenValidityInSeconds()).willReturn(1209600L);
        given(userConditionRepository.existsByUserId(any())).willReturn(false);

        // when
        LoginResult result = authService.verifyEmail(new VerifyEmailRequest(token));

        // then
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(authToken.isUsed()).isTrue();
        assertThat(result.getAccessToken()).isEqualTo("mock-access-token");
        assertThat(result.getUser().getEmail()).isEqualTo("test@example.com");
        assertThat(result.getUser().isOnboarded()).isFalse();
    }

    @Test
    @DisplayName("로그인 성공 - ACTIVE 계정 정상 로그인 및 토큰 발급")
    void login_Success() {
        // given
        LoginRequest request = new LoginRequest("test@example.com", "Password123!");
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .nickname("테스터")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();

        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.of(user));
        given(passwordEncoder.matches("Password123!", "encodedPassword")).willReturn(true);
        given(jwtProvider.createAccessToken(any(), any(), any())).willReturn("mock-access-token");
        given(jwtProvider.createRefreshToken(any())).willReturn("mock-refresh-token");
        given(jwtProvider.getAccessTokenValidityInSeconds()).willReturn(1800L);
        given(jwtProvider.getRefreshTokenValidityInSeconds()).willReturn(1209600L);
        given(userConditionRepository.existsByUserId(any())).willReturn(true);

        // when
        LoginResult result = authService.login(request);

        // then
        assertThat(result.getAccessToken()).isEqualTo("mock-access-token");
        assertThat(result.getRefreshToken()).isEqualTo("mock-refresh-token");
        assertThat(result.getUser().isOnboarded()).isTrue();
        assertThat(user.getLoginFailCount()).isZero();
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치 시 실패 횟수 증가")
    void login_PasswordMismatch() {
        // given
        LoginRequest request = new LoginRequest("test@example.com", "WrongPassword!");
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .nickname("테스터")
                .status(UserStatus.ACTIVE)
                .build();

        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.of(user));
        given(passwordEncoder.matches("WrongPassword!", "encodedPassword")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_INVALID_CREDENTIALS);

        assertThat(user.getLoginFailCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("로그인 실패 - PENDING 계정 로그인 시 AUTH_EMAIL_NOT_VERIFIED")
    void login_PendingUser() {
        // given
        LoginRequest request = new LoginRequest("test@example.com", "Password123!");
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .nickname("테스터")
                .status(UserStatus.PENDING)
                .build();

        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.of(user));
        given(passwordEncoder.matches("Password123!", "encodedPassword")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_EMAIL_NOT_VERIFIED);
    }

    @Test
    @DisplayName("소셜 회원가입 성공 - 유효한 토큰과 미중복 닉네임으로 계정 생성 및 로그인")
    void oauthSignup_Success() {
        // given
        com.ssafy.nyamnyam.domain.auth.dto.request.OAuthSignupRequest request =
                new com.ssafy.nyamnyam.domain.auth.dto.request.OAuthSignupRequest("valid-signup-token", "구글유저");

        io.jsonwebtoken.Claims claims = org.mockito.Mockito.mock(io.jsonwebtoken.Claims.class);
        given(claims.getSubject()).willReturn("OAUTH_SIGNUP");
        given(claims.get("provider", String.class)).willReturn("GOOGLE");
        given(claims.get("providerUserId", String.class)).willReturn("google-12345");
        given(claims.get("email", String.class)).willReturn("googleuser@gmail.com");

        given(jwtProvider.validateToken("valid-signup-token")).willReturn(true);
        given(jwtProvider.parseClaims("valid-signup-token")).willReturn(claims);
        given(userRepository.existsByNickname("구글유저")).willReturn(false);
        given(socialAccountRepository.findByProviderAndProviderUserId(any(), any())).willReturn(Optional.empty());
        given(userRepository.existsByEmail("googleuser@gmail.com")).willReturn(false);

        User savedUser = User.builder()
                .email("googleuser@gmail.com")
                .nickname("구글유저")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(jwtProvider.createAccessToken(any(), any(), any())).willReturn("access-token");
        given(jwtProvider.createRefreshToken(any())).willReturn("refresh-token");
        given(jwtProvider.getAccessTokenValidityInSeconds()).willReturn(1800L);
        given(jwtProvider.getRefreshTokenValidityInSeconds()).willReturn(1209600L);
        given(userConditionRepository.existsByUserId(any())).willReturn(false);

        // when
        LoginResult result = authService.oauthSignup(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("access-token");
        assertThat(result.getUser().getEmail()).isEqualTo("googleuser@gmail.com");
        assertThat(result.getUser().getNickname()).isEqualTo("구글유저");
        verify(socialAccountRepository).save(any());
    }

    @Test
    @DisplayName("소셜 회원가입 실패 - 닉네임 중복 시 AUTH_NICKNAME_DUPLICATED")
    void oauthSignup_NicknameDuplicated() {
        // given
        com.ssafy.nyamnyam.domain.auth.dto.request.OAuthSignupRequest request =
                new com.ssafy.nyamnyam.domain.auth.dto.request.OAuthSignupRequest("valid-signup-token", "중복닉네임");

        io.jsonwebtoken.Claims claims = org.mockito.Mockito.mock(io.jsonwebtoken.Claims.class);
        given(claims.getSubject()).willReturn("OAUTH_SIGNUP");
        given(claims.get("provider", String.class)).willReturn("GOOGLE");
        given(claims.get("providerUserId", String.class)).willReturn("google-12345");
        given(claims.get("email", String.class)).willReturn("googleuser@gmail.com");

        given(jwtProvider.validateToken("valid-signup-token")).willReturn(true);
        given(jwtProvider.parseClaims("valid-signup-token")).willReturn(claims);
        given(userRepository.existsByNickname("중복닉네임")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.oauthSignup(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AUTH_NICKNAME_DUPLICATED);
    }
}

