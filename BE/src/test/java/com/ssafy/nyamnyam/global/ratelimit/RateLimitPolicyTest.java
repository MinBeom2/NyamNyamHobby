package com.ssafy.nyamnyam.global.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitPolicyTest {

    @Test
    @DisplayName("메일을 보내는 인증 API는 AUTH_MAIL, 나머지 인증 API는 AUTH")
    void auth() {
        assertThat(RateLimitPolicy.resolve("POST", "/api/v1/auth/signup")).isEqualTo(RateLimitPolicy.AUTH_MAIL);
        assertThat(RateLimitPolicy.resolve("POST", "/api/v1/auth/email/resend")).isEqualTo(RateLimitPolicy.AUTH_MAIL);
        assertThat(RateLimitPolicy.resolve("POST", "/api/v1/auth/password/reset-request")).isEqualTo(RateLimitPolicy.AUTH_MAIL);

        assertThat(RateLimitPolicy.resolve("POST", "/api/v1/auth/login")).isEqualTo(RateLimitPolicy.AUTH);
        assertThat(RateLimitPolicy.resolve("POST", "/api/v1/auth/refresh")).isEqualTo(RateLimitPolicy.AUTH);
        assertThat(RateLimitPolicy.resolve("GET", "/api/v1/auth/check-email")).isEqualTo(RateLimitPolicy.AUTH);
    }

    @Test
    @DisplayName("GMS를 부르는 추천 생성은 AI, 미리보기는 일반 쓰기")
    void ai() {
        assertThat(RateLimitPolicy.resolve("POST", "/api/v1/recommendations")).isEqualTo(RateLimitPolicy.AI);
        assertThat(RateLimitPolicy.resolve("POST", "/api/v1/recommendations/preview")).isEqualTo(RateLimitPolicy.WRITE);
        assertThat(RateLimitPolicy.resolve("GET", "/api/v1/recommendations/3")).isEqualTo(RateLimitPolicy.READ);
    }

    @Test
    @DisplayName("업로드·게시글 작성·댓글 작성은 각각의 정책")
    void uploadAndContent() {
        assertThat(RateLimitPolicy.resolve("POST", "/api/v1/uploads/images")).isEqualTo(RateLimitPolicy.UPLOAD);
        assertThat(RateLimitPolicy.resolve("POST", "/api/v1/posts")).isEqualTo(RateLimitPolicy.CONTENT);
        assertThat(RateLimitPolicy.resolve("POST", "/api/v1/posts/12/comments")).isEqualTo(RateLimitPolicy.CONTENT);

        assertThat(RateLimitPolicy.resolve("PATCH", "/api/v1/posts/12")).isEqualTo(RateLimitPolicy.WRITE);
        assertThat(RateLimitPolicy.resolve("PUT", "/api/v1/posts/12/like")).isEqualTo(RateLimitPolicy.WRITE);
        assertThat(RateLimitPolicy.resolve("GET", "/api/v1/posts/12/comments")).isEqualTo(RateLimitPolicy.READ);
        assertThat(RateLimitPolicy.resolve("POST", "/api/v1/posts/12/comments/extra")).isEqualTo(RateLimitPolicy.WRITE);
    }

    @Test
    @DisplayName("GET·HEAD는 조회, 모르는 메서드는 쓰기로 안전하게")
    void readAndWrite() {
        assertThat(RateLimitPolicy.resolve("GET", "/api/v1/hobbies")).isEqualTo(RateLimitPolicy.READ);
        assertThat(RateLimitPolicy.resolve("head", "/api/v1/hobbies")).isEqualTo(RateLimitPolicy.READ);
        assertThat(RateLimitPolicy.resolve("DELETE", "/api/v1/comments/3")).isEqualTo(RateLimitPolicy.WRITE);
        assertThat(RateLimitPolicy.resolve("PROPFIND", "/api/v1/hobbies")).isEqualTo(RateLimitPolicy.WRITE);
    }

    @Test
    @DisplayName("인증 정책만 IP 고정, 돈이 드는 정책만 Redis 장애 시 차단")
    void flags() {
        assertThat(RateLimitPolicy.AUTH_MAIL.ipOnly()).isTrue();
        assertThat(RateLimitPolicy.AUTH.ipOnly()).isTrue();
        assertThat(RateLimitPolicy.READ.ipOnly()).isFalse();

        assertThat(RateLimitPolicy.AUTH_MAIL.rejectWhenUnavailable()).isTrue();
        assertThat(RateLimitPolicy.AI.rejectWhenUnavailable()).isTrue();
        assertThat(RateLimitPolicy.AUTH.rejectWhenUnavailable()).isFalse();
        assertThat(RateLimitPolicy.READ.rejectWhenUnavailable()).isFalse();

        assertThat(RateLimitPolicy.AI.period()).isEqualTo(Duration.ofHours(1));
    }
}
