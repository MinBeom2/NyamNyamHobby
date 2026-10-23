package com.ssafy.nyamnyam.global.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HexFormat;

@Slf4j
@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final long accessTokenValidityInSeconds;
    private final long refreshTokenValidityInSeconds;

    public JwtProvider(
            @Value("${jwt.secret:}") String secret,
            @Value("${jwt.access-token-validity-in-seconds:1800}") long accessTokenValidityInSeconds,
            @Value("${jwt.refresh-token-validity-in-seconds:1209600}") long refreshTokenValidityInSeconds
    ) {
        String effectiveSecret = (secret != null && secret.getBytes(StandardCharsets.UTF_8).length >= 32)
                ? secret
                : "nyamnyam-secret-key-must-be-at-least-256-bits-long-for-hmac-sha256";
        this.secretKey = Keys.hmacShaKeyFor(effectiveSecret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityInSeconds = accessTokenValidityInSeconds;
        this.refreshTokenValidityInSeconds = refreshTokenValidityInSeconds;
    }

    public String createAccessToken(Long userId, String email, String role) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + (this.accessTokenValidityInSeconds * 1000));

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(validity)
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + (this.refreshTokenValidityInSeconds * 1000));

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(validity)
                .signWith(secretKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("잘못된 JWT 서명입니다: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰입니다: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 비어있거나 잘못되었습니다: {}", e.getMessage());
        }
        return false;
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    public long getAccessTokenValidityInSeconds() {
        return accessTokenValidityInSeconds;
    }

    public long getRefreshTokenValidityInSeconds() {
        return refreshTokenValidityInSeconds;
    }

    /**
     * SHA-256으로 토큰을 해싱하여 64자리 16진수 문자열 반환 (auth_tokens 테이블 저장용)
     */
    public static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }
}
