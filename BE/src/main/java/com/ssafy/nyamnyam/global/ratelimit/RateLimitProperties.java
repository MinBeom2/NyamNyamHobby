package com.ssafy.nyamnyam.global.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * 레이트 리밋 설정(app.rate-limit.*).
 *
 * @param enabled           false면 필터가 아무것도 검사하지 않는다(로컬 테스트·시연용)
 * @param clientIpHeader    프록시가 실제 클라이언트 IP를 넣어 주는 헤더. 서버 Nginx는 X-Real-IP를 $remote_addr로 덮어쓴다
 * @param trustedProxyCidrs 이 대역에서 들어온 요청만 clientIpHeader를 믿는다.
 *                          백엔드는 127.0.0.1:18080으로만 열려 있어 Nginx(도커 게이트웨이 172.x)만 접근할 수 있다
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("X-Real-IP") String clientIpHeader,
        @DefaultValue({"127.0.0.0/8", "::1/128", "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16"})
        List<String> trustedProxyCidrs
) {
}
