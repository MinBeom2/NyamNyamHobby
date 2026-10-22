package com.ssafy.nyamnyam.domain.auth.service;

import com.ssafy.nyamnyam.domain.auth.dto.request.*;
import com.ssafy.nyamnyam.domain.auth.dto.response.*;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserConditionRepository userConditionRepository;
    private final AuthTokenRepository authTokenRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final EmailService emailService;

    /**
     * 1) 이메일 회원가입: User(PENDING) 생성 → 이메일 인증 토큰 발급 → 인증 메일 발송
     */
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        // 이메일 중복 검사 (소셜 가입 계정이면 AUTH_SOCIAL_ACCOUNT 반환)
        userRepository.findByEmail(request.getEmail()).ifPresent(existingUser -> {
            if (existingUser.getPasswordHash() == null || socialAccountRepository.existsByUserId(existingUser.getId())) {
                throw new CustomException(ErrorCode.AUTH_SOCIAL_ACCOUNT, "Google로 가입된 이메일이에요. Google 로그인을 이용해 주세요.");
            }
            throw new CustomException(ErrorCode.AUTH_EMAIL_DUPLICATED);
        });

        // 닉네임 중복 검사
        if (userRepository.existsByNickname(request.getNickname())) {
            throw new CustomException(ErrorCode.AUTH_NICKNAME_DUPLICATED);
        }

        // 비밀번호 해싱 후 User 엔티티 생성
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .role(Role.USER)
                .status(UserStatus.PENDING)
                .build();
        userRepository.save(user);

        // 이메일 인증 토큰 생성 & 발송
        sendEmailVerificationToken(user);

        return new SignupResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getStatus().name(),
                "인증 메일이 발송되었습니다. 24시간 내에 링크를 확인해 주세요."
        );
    }

    /**
     * 2) 이메일 중복 확인
     */
    @Transactional(readOnly = true)
    public CheckEmailResponse checkEmail(String email) {
        boolean available = !userRepository.existsByEmail(email);
        return new CheckEmailResponse(email, available);
    }

    /**
     * 3) 닉네임 중복 확인
     */
    @Transactional(readOnly = true)
    public CheckNicknameResponse checkNickname(String nickname) {
        boolean available = !userRepository.existsByNickname(nickname);
        return new CheckNicknameResponse(nickname, available);
    }

    /**
     * 4) 이메일 인증: 토큰 검증 → User ACTIVE 전환 → 바로 로그인 처리 (토큰 발급)
     */
    @Transactional
    public LoginResult verifyEmail(VerifyEmailRequest request) {
        String tokenHash = JwtProvider.hashToken(request.getToken());
        AuthToken authToken = authTokenRepository
                .findByTokenHashAndType(tokenHash, TokenType.EMAIL_VERIFY)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_LINK_INVALID));

        LocalDateTime now = LocalDateTime.now();

        if (authToken.isUsed()) {
            throw new CustomException(ErrorCode.AUTH_LINK_INVALID, "이미 사용된 인증 링크입니다.");
        }
        if (authToken.isExpired(now)) {
            throw new CustomException(ErrorCode.AUTH_LINK_INVALID, "인증 링크가 만료되었습니다. 인증 메일을 재발송해 주세요.");
        }

        // 토큰 사용 처리 & 유저 활성화
        authToken.markUsed(now);
        User user = authToken.getUser();
        user.activate(now);

        log.info("[Auth] 이메일 인증 완료: userId={}, email={}", user.getId(), user.getEmail());

        // 로그인 토큰 발급
        return issueTokens(user, now);
    }

    /**
     * 인증 메일 재발송 (60초 쿨다운 적용)
     */
    @Transactional
    public void resendVerificationEmail(ResendEmailRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new CustomException(ErrorCode.AUTH_LINK_INVALID, "이미 인증된 계정입니다.");
        }

        // 쿨다운 검사: 마지막 토큰 생성 후 60초 이내이면 거부
        authTokenRepository.findFirstByUserIdAndTypeOrderByCreatedAtDesc(user.getId(), TokenType.EMAIL_VERIFY)
                .ifPresent(lastToken -> {
                    if (lastToken.getCreatedAt().plusSeconds(60).isAfter(LocalDateTime.now())) {
                        throw new CustomException(ErrorCode.AUTH_RESEND_COOLDOWN, 60);
                    }
                });

        sendEmailVerificationToken(user);
    }

    /**
     * 5) 이메일 로그인: 자격증명 검증 → 계정 잠금 확인 → Access + Refresh 토큰 발급
     */
    @Transactional
    public LoginResult login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        // 소셜 전용 계정인지 확인
        if (user.getPasswordHash() == null) {
            throw new CustomException(ErrorCode.AUTH_SOCIAL_ACCOUNT, "Google로 가입된 계정이에요. Google 로그인을 이용해 주세요.");
        }

        LocalDateTime now = LocalDateTime.now();

        // 계정 잠금 확인
        if (user.isLocked(now)) {
            throw new CustomException(ErrorCode.AUTH_ACCOUNT_LOCKED);
        }

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            user.incrementLoginFail(now);
            throw new CustomException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        // 이메일 미인증 확인
        if (user.getStatus() == UserStatus.PENDING) {
            throw new CustomException(ErrorCode.AUTH_EMAIL_NOT_VERIFIED);
        }

        // 탈퇴 계정 확인
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new CustomException(ErrorCode.AUTH_INVALID_CREDENTIALS, "탈퇴한 계정입니다.");
        }

        // 로그인 성공: 실패 카운트 초기화
        user.resetLoginFail(now);

        log.info("[Auth] 로그인 성공: userId={}, email={}", user.getId(), user.getEmail());

        return issueTokens(user, now);
    }

    /**
     * 6) Refresh Token으로 Access Token 재발급
     */
    @Transactional
    public TokenRefreshResponse refreshAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new CustomException(ErrorCode.AUTH_REFRESH_INVALID);
        }

        // 1) JWT 유효성 검증
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.AUTH_REFRESH_INVALID);
        }

        // 2) DB에 저장된 해시와 비교
        String tokenHash = JwtProvider.hashToken(refreshToken);
        AuthToken storedToken = authTokenRepository
                .findByTokenHashAndType(tokenHash, TokenType.REFRESH)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_REFRESH_INVALID));

        LocalDateTime now = LocalDateTime.now();
        if (storedToken.isUsed() || storedToken.isExpired(now)) {
            throw new CustomException(ErrorCode.AUTH_REFRESH_INVALID);
        }

        // 3) 새 Access Token 발급
        Long userId = jwtProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String newAccessToken = jwtProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name());

        return new TokenRefreshResponse(newAccessToken, jwtProvider.getAccessTokenValidityInSeconds());
    }

    /**
     * 7) 로그아웃: DB에서 Refresh Token 폐기
     */
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        String tokenHash = JwtProvider.hashToken(refreshToken);
        authTokenRepository.findByTokenHashAndType(tokenHash, TokenType.REFRESH)
                .ifPresent(token -> token.markUsed(LocalDateTime.now()));
    }

    /**
     * 8) 비밀번호 재설정 메일 요청
     */
    @Transactional
    public MessageResponse requestPasswordReset(PasswordResetRequest request) {
        String email = request.getEmail();
        userRepository.findByEmail(email).ifPresent(user -> {
            // 소셜 전용 계정이면 Google 로그인 이용 안내 메일
            if (user.getPasswordHash() == null || socialAccountRepository.existsByUserId(user.getId())) {
                emailService.sendGoogleAccountNoticeEmail(email);
                return;
            }

            // 30분 유효기간 일회용 토큰 생성
            String rawToken = UUID.randomUUID().toString();
            String tokenHash = JwtProvider.hashToken(rawToken);

            AuthToken authToken = AuthToken.builder()
                    .user(user)
                    .type(TokenType.PASSWORD_RESET)
                    .tokenHash(tokenHash)
                    .expiresAt(LocalDateTime.now().plusMinutes(30))
                    .build();
            authTokenRepository.save(authToken);

            emailService.sendPasswordResetEmail(email, rawToken);
        });

        // 보안상 사용자 존재 여부와 무관하게 동일 응답 반환
        return new MessageResponse("입력하신 이메일로 비밀번호 재설정 링크를 발송했습니다.");
    }

    /**
     * 9) 비밀번호 재설정 실행
     */
    @Transactional
    public MessageResponse resetPassword(PasswordResetConfirmRequest request) {
        String tokenHash = JwtProvider.hashToken(request.getToken());
        AuthToken authToken = authTokenRepository
                .findByTokenHashAndType(tokenHash, TokenType.PASSWORD_RESET)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_LINK_INVALID));

        LocalDateTime now = LocalDateTime.now();

        if (authToken.isUsed()) {
            throw new CustomException(ErrorCode.AUTH_LINK_INVALID, "이미 사용된 비밀번호 재설정 링크입니다.");
        }
        if (authToken.isExpired(now)) {
            throw new CustomException(ErrorCode.AUTH_LINK_INVALID, "비밀번호 재설정 링크가 만료되었습니다. 다시 요청해 주세요.");
        }

        User user = authToken.getUser();

        // 새 비밀번호 변경
        user.changePassword(passwordEncoder.encode(request.getNewPassword()));

        // 기존 모든 Refresh 토큰 폐기 (보안)
        authTokenRepository.deleteAllByUserIdAndType(user.getId(), TokenType.REFRESH);

        // 토큰 사용 처리
        authToken.markUsed(now);

        log.info("[Auth] 비밀번호 재설정 완료: userId={}, email={}", user.getId(), user.getEmail());

        return new MessageResponse("비밀번호가 성공적으로 변경되었습니다. 다시 로그인해 주세요.");
    }

    // ─── Private Helpers ───

    private LoginResult issueTokens(User user, LocalDateTime now) {
        String accessToken = jwtProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        // 기존 Refresh Token 폐기 후 새로 저장
        authTokenRepository.deleteAllByUserIdAndType(user.getId(), TokenType.REFRESH);
        AuthToken refreshTokenEntity = AuthToken.builder()
                .user(user)
                .type(TokenType.REFRESH)
                .tokenHash(JwtProvider.hashToken(refreshToken))
                .expiresAt(now.plusSeconds(jwtProvider.getRefreshTokenValidityInSeconds()))
                .build();
        authTokenRepository.save(refreshTokenEntity);

        boolean onboarded = userConditionRepository.existsByUserId(user.getId());
        UserSummaryResponse userSummary = UserSummaryResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole().name())
                .onboarded(onboarded)
                .build();

        return new LoginResult(accessToken, refreshToken, jwtProvider.getAccessTokenValidityInSeconds(), userSummary);
    }

    private void sendEmailVerificationToken(User user) {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = JwtProvider.hashToken(rawToken);

        AuthToken authToken = AuthToken.builder()
                .user(user)
                .type(TokenType.EMAIL_VERIFY)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusHours(24)) // 24시간 유효
                .build();
        authTokenRepository.save(authToken);

        emailService.sendVerificationEmail(user.getEmail(), rawToken);
    }
}
