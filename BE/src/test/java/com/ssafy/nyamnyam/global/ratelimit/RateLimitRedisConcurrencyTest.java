package com.ssafy.nyamnyam.global.ratelimit;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 Redis에서 동시 요청이 한도를 넘지 않는지 확인한다.
 * 로컬 Redis(docker compose)를 켠 뒤 RATE_LIMIT_REDIS_TESTS=true 로 실행한다. 접속 정보는 REDIS_HOST·REDIS_PORT·REDIS_PASSWORD.
 */
@EnabledIfEnvironmentVariable(named = "RATE_LIMIT_REDIS_TESTS", matches = "true")
class RateLimitRedisConcurrencyTest {

    private static RedisClient client;
    private static Bucket4jRateLimitService service;

    @BeforeAll
    static void connect() {
        RedisURI.Builder uri = RedisURI.builder()
                .withHost(env("REDIS_HOST", "localhost"))
                .withPort(Integer.parseInt(env("REDIS_PORT", "6379")));
        String password = System.getenv("REDIS_PASSWORD");
        if (password != null && !password.isBlank()) {
            uri.withPassword(password.toCharArray());
        }
        client = RedisClient.create(uri.build());
        // 운영과 같은 설정(RateLimitConfig)으로 만든다
        service = new Bucket4jRateLimitService(() -> RateLimitConfig.createProxyManager(client));
    }

    @AfterAll
    static void close() {
        if (client != null) {
            client.shutdown();
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    @Test
    @DisplayName("한 사용자가 AI 추천(시간당 10회)을 100번 동시에 요청하면 정확히 10번만 통과한다")
    void concurrentRequestsNeverExceedCapacity() throws Exception {
        RateLimitIdentity user = RateLimitIdentity.user(Math.abs(UUID.randomUUID().getMostSignificantBits()));
        int requests = 100;
        ExecutorService pool = Executors.newFixedThreadPool(32);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        for (int i = 0; i < requests; i++) {
            results.add(pool.submit(() -> {
                start.await();
                return service.consume(RateLimitPolicy.AI, user).allowed();
            }));
        }
        start.countDown();

        long allowed = 0;
        for (Future<Boolean> result : results) {
            if (result.get()) {
                allowed++;
            }
        }
        pool.shutdown();

        assertThat(allowed).isEqualTo(RateLimitPolicy.AI.capacity());
    }

    @Test
    @DisplayName("다른 사용자의 버킷은 서로 영향을 주지 않는다")
    void bucketsAreIsolated() {
        RateLimitIdentity a = RateLimitIdentity.user(Math.abs(UUID.randomUUID().getMostSignificantBits()));
        RateLimitIdentity b = RateLimitIdentity.user(Math.abs(UUID.randomUUID().getMostSignificantBits()));

        for (int i = 0; i < RateLimitPolicy.CONTENT.capacity(); i++) {
            assertThat(service.consume(RateLimitPolicy.CONTENT, a).allowed()).isTrue();
        }
        RateLimitDecision rejected = service.consume(RateLimitPolicy.CONTENT, a);

        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.retryAfterSeconds()).isBetween(1L, 6L);
        assertThat(service.consume(RateLimitPolicy.CONTENT, b).allowed()).isTrue();
    }
}
