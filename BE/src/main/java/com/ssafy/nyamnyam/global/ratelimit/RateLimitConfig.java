package com.ssafy.nyamnyam.global.ratelimit;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.RedisClient;
import io.lettuce.core.cluster.RedisClusterClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

/**
 * 레이트 리밋 빈. 필터는 SecurityConfig가 JWT 필터 뒤에 끼운다.
 * <p>
 * Bucket4j는 Lettuce 클라이언트가 필요한데, 새로 만들지 않고 Spring이 만든 Redis 연결(LettuceConnectionFactory)의
 * 클라이언트를 그대로 쓴다. 그래서 spring.data.redis.* 설정(host·port·password)을 따로 적지 않아도 된다.
 */
@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {

    /** 버킷이 가득 찬 뒤 이만큼 더 지나면 Redis 키가 사라진다(가득 찬 버킷은 없는 버킷과 같으므로) */
    private static final Duration KEY_EXPIRY_MARGIN = Duration.ofSeconds(10);
    /**
     * CAS 충돌 시 재시도 횟수. 너무 작으면 한 키에 요청이 몰릴 때(공격 상황) 재시도가 바닥나 예외 → fail-open으로
     * 오히려 통과되므로 넉넉하게 둔다. 매 라운드 최소 한 요청은 성공하므로 무한 반복되지는 않는다.
     */
    static final int MAX_CAS_RETRIES = 100;

    @Bean
    public RateLimitService rateLimitService(LettuceConnectionFactory connectionFactory) {
        return new Bucket4jRateLimitService(() -> createProxyManager(connectionFactory.getRequiredNativeClient()));
    }

    @Bean
    public ClientIdentityResolver clientIdentityResolver(RateLimitProperties properties) {
        return new ClientIdentityResolver(properties);
    }

    @Bean
    public RateLimitFilter rateLimitFilter(RateLimitProperties properties,
                                           ClientIdentityResolver clientIdentityResolver,
                                           RateLimitService rateLimitService,
                                           ObjectMapper objectMapper) {
        return new RateLimitFilter(properties, clientIdentityResolver, rateLimitService, objectMapper);
    }

    /** 운영 설정 그대로 ProxyManager를 만든다(동시성 테스트도 이 메서드를 쓴다) */
    static ProxyManager<byte[]> createProxyManager(AbstractRedisClient client) {
        ExpirationAfterWriteStrategy expiration =
                ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(KEY_EXPIRY_MARGIN);

        if (client instanceof RedisClient redisClient) {
            return Bucket4jLettuce.casBasedBuilder(redisClient)
                    .expirationAfterWrite(expiration)
                    .maxRetries(MAX_CAS_RETRIES)
                    .build();
        }
        if (client instanceof RedisClusterClient clusterClient) {
            return Bucket4jLettuce.casBasedBuilder(clusterClient)
                    .expirationAfterWrite(expiration)
                    .maxRetries(MAX_CAS_RETRIES)
                    .build();
        }
        throw new IllegalStateException("지원하지 않는 Redis 클라이언트입니다: " + client.getClass().getName());
    }
}
