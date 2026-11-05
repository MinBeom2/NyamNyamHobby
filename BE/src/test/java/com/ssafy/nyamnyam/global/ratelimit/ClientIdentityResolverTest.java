package com.ssafy.nyamnyam.global.ratelimit;

import com.ssafy.nyamnyam.global.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIdentityResolverTest {

    private final ClientIdentityResolver resolver = new ClientIdentityResolver(defaults());

    static RateLimitProperties defaults() {
        return new RateLimitProperties(true, "X-Real-IP",
                List.of("127.0.0.0/8", "::1/128", "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16"));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static void login(long userId) {
        UserPrincipal principal = new UserPrincipal(userId, "u@test.com", "USER");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private static MockHttpServletRequest request(String peer, String realIp) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/hobbies");
        request.setRemoteAddr(peer);
        if (realIp != null) {
            request.addHeader("X-Real-IP", realIp);
        }
        return request;
    }

    @Test
    @DisplayName("로그인 사용자는 사용자 ID로 센다")
    void user() {
        login(15L);

        RateLimitIdentity identity = resolver.resolve(request("172.18.0.1", "203.0.113.10"), RateLimitPolicy.READ);

        assertThat(identity).isEqualTo(RateLimitIdentity.user(15L));
        assertThat(identity.keyPart()).isEqualTo("user:15");
    }

    @Test
    @DisplayName("인증 API는 로그인해 있어도 IP로 센다")
    void authIsIpOnly() {
        login(15L);

        RateLimitIdentity identity = resolver.resolve(request("172.18.0.1", "203.0.113.10"), RateLimitPolicy.AUTH);

        assertThat(identity).isEqualTo(RateLimitIdentity.ip("203.0.113.10"));
    }

    @Test
    @DisplayName("Nginx(도커 게이트웨이)에서 온 요청은 X-Real-IP를 클라이언트 IP로 쓴다")
    void trustedProxy() {
        assertThat(resolver.resolve(request("172.18.0.1", "203.0.113.10"), RateLimitPolicy.READ).value())
                .isEqualTo("203.0.113.10");
    }

    @Test
    @DisplayName("신뢰하지 않는 주소에서 직접 온 요청의 X-Real-IP는 무시한다(위조 방지)")
    void untrustedPeerCannotSpoof() {
        assertThat(resolver.resolve(request("198.51.100.7", "1.2.3.4"), RateLimitPolicy.READ).value())
                .isEqualTo("198.51.100.7");
    }

    @Test
    @DisplayName("ForwardedHeaderFilter가 감싸 getRemoteAddr()를 X-Forwarded-For 값으로 바꿔도 원래 접속 주소를 본다")
    void unwrapsForwardedRequest() {
        MockHttpServletRequest original = request("198.51.100.7", "1.2.3.4");
        HttpServletRequest wrapped = new HttpServletRequestWrapper(original) {
            @Override
            public String getRemoteAddr() {
                return "10.0.0.5"; // 클라이언트가 X-Forwarded-For에 넣은 값이라고 가정
            }
        };

        assertThat(resolver.clientIp(wrapped)).isEqualTo("198.51.100.7");
    }

    @Test
    @DisplayName("헤더가 없거나 IP 형식이 아니면 접속 주소를 쓴다")
    void fallbackToPeer() {
        assertThat(resolver.clientIp(request("127.0.0.1", null))).isEqualTo("127.0.0.1");
        assertThat(resolver.clientIp(request("172.18.0.1", "evil.example.com"))).isEqualTo("172.18.0.1");
        assertThat(resolver.clientIp(request("172.18.0.1", "999.1.1.1"))).isEqualTo("172.18.0.1");
    }

    @Test
    @DisplayName("값이 여러 개면 프록시가 직접 본 마지막 값을 쓴다")
    void multipleValues() {
        assertThat(resolver.clientIp(request("172.18.0.1", "1.2.3.4, 203.0.113.10"))).isEqualTo("203.0.113.10");
    }

    @Test
    @DisplayName("IP 형식 검사")
    void normalizeIp() {
        assertThat(ClientIdentityResolver.normalizeIp("203.0.113.10")).isEqualTo("203.0.113.10");
        assertThat(ClientIdentityResolver.normalizeIp("::1")).isEqualTo("0:0:0:0:0:0:0:1");
        assertThat(ClientIdentityResolver.normalizeIp("1.2.3")).isNull();
        assertThat(ClientIdentityResolver.normalizeIp("localhost")).isNull();
        assertThat(ClientIdentityResolver.normalizeIp(" ")).isNull();
    }
}
