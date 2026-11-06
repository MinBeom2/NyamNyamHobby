package com.ssafy.nyamnyam.global.ratelimit;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private final AtomicBoolean chainCalled = new AtomicBoolean();
    private final FilterChain chain = (req, res) -> chainCalled.set(true);

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static RateLimitFilter filter(boolean enabled, RateLimitService service) {
        RateLimitProperties properties = new RateLimitProperties(enabled, "X-Real-IP", List.of("127.0.0.0/8"));
        return new RateLimitFilter(properties, new ClientIdentityResolver(properties), service, JsonMapper.builder().build());
    }

    private static MockHttpServletRequest request(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Real-IP", "203.0.113.10");
        return request;
    }

    @Test
    @DisplayName("허용: 남은 한도 헤더를 달고 다음 필터로 넘긴다")
    void allowed() throws Exception {
        AtomicReference<RateLimitIdentity> seen = new AtomicReference<>();
        RateLimitFilter filter = filter(true, (policy, identity) -> {
            seen.set(identity);
            return new RateLimitDecision(true, policy, identity.type(), policy.capacity(), 299, 0);
        });
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("GET", "/api/v1/hobbies"), response, chain);

        assertThat(chainCalled).isTrue();
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("300");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("299");
        assertThat(seen.get()).isEqualTo(RateLimitIdentity.ip("203.0.113.10"));
    }

    @Test
    @DisplayName("초과: 429 + Retry-After + 공통 오류 본문, 컨트롤러까지 가지 않는다")
    void exceeded() throws Exception {
        RateLimitFilter filter = filter(true,
                (policy, identity) -> new RateLimitDecision(false, policy, identity.type(), policy.capacity(), 0, 6));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("POST", "/api/v1/posts/3/comments"), response, chain);

        assertThat(chainCalled).isFalse();
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("6");
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("10");
        assertThat(response.getContentAsString())
                .contains("\"success\":false")
                .contains("\"code\":\"RATE_LIMIT_EXCEEDED\"")
                .contains("\"retryAfterSeconds\":6");
    }

    @Test
    @DisplayName("Redis 장애: 돈이 드는 요청(GMS 추천)은 503으로 막는다")
    void unavailableRejectsCostlyRequest() throws Exception {
        RateLimitFilter filter = filter(true, (policy, identity) -> {
            throw new RateLimitUnavailableException(new IllegalStateException("redis down"));
        });
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("POST", "/api/v1/recommendations"), response, chain);

        assertThat(chainCalled).isFalse();
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("\"code\":\"RATE_LIMIT_UNAVAILABLE\"");
    }

    @Test
    @DisplayName("Redis 장애: 일반 요청은 통과시킨다(fail-open)")
    void unavailableAllowsOthers() throws Exception {
        RateLimitFilter filter = filter(true, (policy, identity) -> {
            throw new RateLimitUnavailableException(new IllegalStateException("redis down"));
        });
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request("GET", "/api/v1/hobbies"), response, chain);

        assertThat(chainCalled).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("세지 않는 요청: API가 아님, CORS 사전 요청, 업로드 이미지 조회, 기능 꺼짐")
    void skipped() throws Exception {
        AtomicBoolean consumed = new AtomicBoolean();
        RateLimitService service = (policy, identity) -> {
            consumed.set(true);
            return new RateLimitDecision(true, policy, identity.type(), 1, 0, 0);
        };

        filter(true, service).doFilter(request("GET", "/oauth2/authorization/google"), new MockHttpServletResponse(), chain);
        filter(true, service).doFilter(request("OPTIONS", "/api/v1/posts"), new MockHttpServletResponse(), chain);
        filter(true, service).doFilter(request("GET", "/api/v1/uploads/images/post/2026/11/a.jpg"), new MockHttpServletResponse(), chain);
        filter(false, service).doFilter(request("POST", "/api/v1/posts"), new MockHttpServletResponse(), chain);

        assertThat(consumed).isFalse();
        assertThat(chainCalled).isTrue();
    }
}
