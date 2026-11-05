package com.ssafy.nyamnyam.global.ratelimit;

import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class Bucket4jRateLimitServiceTest {

    private final RateLimitIdentity ip = RateLimitIdentity.ip("203.0.113.10");

    @Test
    @DisplayName("ProxyManager는 첫 요청 때 한 번만 만들고, 정책·기준별 키로 버킷을 찾는다")
    @SuppressWarnings("unchecked")
    void lazyInitAndKey() {
        ProxyManager<byte[]> proxyManager = mock(ProxyManager.class);
        BucketProxy bucket = mock(BucketProxy.class);
        byte[] key = "rate-limit:v1:auth:ip:203.0.113.10".getBytes(StandardCharsets.UTF_8);
        when(proxyManager.getProxy(eq(key), any(Supplier.class))).thenReturn(bucket);
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(ConsumptionProbe.consumed(59, 0));
        AtomicInteger created = new AtomicInteger();
        Bucket4jRateLimitService service = new Bucket4jRateLimitService(() -> {
            created.incrementAndGet();
            return proxyManager;
        });

        assertThat(created).hasValue(0);
        RateLimitDecision first = service.consume(RateLimitPolicy.AUTH, ip);
        service.consume(RateLimitPolicy.AUTH, ip);

        assertThat(created).hasValue(1);
        assertThat(first.allowed()).isTrue();
        assertThat(first.limit()).isEqualTo(60);
        assertThat(first.remaining()).isEqualTo(59);
        assertThat(first.retryAfterSeconds()).isZero();
    }

    @Test
    @DisplayName("토큰이 없으면 거절하고 다음 토큰까지 남은 시간을 올림한 초로 알려 준다")
    @SuppressWarnings("unchecked")
    void rejected() {
        ProxyManager<byte[]> proxyManager = mock(ProxyManager.class);
        BucketProxy bucket = mock(BucketProxy.class);
        when(proxyManager.getProxy(any(byte[].class), any(Supplier.class))).thenReturn(bucket);
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(ConsumptionProbe.rejected(0, 5_200_000_000L, 5_200_000_000L));

        RateLimitDecision decision = new Bucket4jRateLimitService(() -> proxyManager).consume(RateLimitPolicy.CONTENT, ip);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.remaining()).isZero();
        assertThat(decision.retryAfterSeconds()).isEqualTo(6);
    }

    @Test
    @DisplayName("Redis 연결에 실패하면 RateLimitUnavailableException으로 바꾸고, 다음 요청에서 다시 연결을 시도한다")
    void unavailable() {
        AtomicInteger attempts = new AtomicInteger();
        Bucket4jRateLimitService service = new Bucket4jRateLimitService(() -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("redis down");
        });

        assertThatThrownBy(() -> service.consume(RateLimitPolicy.READ, ip))
                .isInstanceOf(RateLimitUnavailableException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.consume(RateLimitPolicy.READ, ip))
                .isInstanceOf(RateLimitUnavailableException.class);
        assertThat(attempts).hasValue(2);
    }

    @Test
    @DisplayName("Retry-After 초 변환: 올림, 최소 1초")
    void retryAfterSeconds() {
        assertThat(Bucket4jRateLimitService.toRetryAfterSeconds(1)).isEqualTo(1);
        assertThat(Bucket4jRateLimitService.toRetryAfterSeconds(1_000_000_000L)).isEqualTo(1);
        assertThat(Bucket4jRateLimitService.toRetryAfterSeconds(1_000_000_001L)).isEqualTo(2);
    }
}
