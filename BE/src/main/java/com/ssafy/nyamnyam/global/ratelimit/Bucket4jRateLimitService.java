package com.ssafy.nyamnyam.global.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

/**
 * Redis에 버킷을 두는 Bucket4j 구현. 서버가 여러 대여도 같은 버킷을 본다.
 * 동시 요청은 Bucket4j의 CAS(읽고, 그 사이 바뀌지 않았으면 쓰고, 바뀌었으면 다시 시도)로 처리한다.
 * <p>
 * ProxyManager는 첫 요청 때 만든다. 앱이 뜰 때 Redis가 없어도 기동은 되고, 실패하면 다음 요청에서 다시 시도한다.
 */
public class Bucket4jRateLimitService implements RateLimitService {

    /** 정책 값을 바꿀 때 v2로 올리면 기존 버킷(옛 설정)과 섞이지 않는다 */
    static final String KEY_PREFIX = "rate-limit:v1:";

    private final Supplier<ProxyManager<byte[]>> proxyManagerSupplier;
    private volatile ProxyManager<byte[]> proxyManager;

    public Bucket4jRateLimitService(Supplier<ProxyManager<byte[]>> proxyManagerSupplier) {
        this.proxyManagerSupplier = proxyManagerSupplier;
    }

    @Override
    public RateLimitDecision consume(RateLimitPolicy policy, RateLimitIdentity identity) {
        try {
            Bucket bucket = proxyManager().getProxy(
                    bucketKey(policy, identity).getBytes(StandardCharsets.UTF_8),
                    () -> configurationFor(policy));
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

            long retryAfterSeconds = probe.isConsumed() ? 0 : toRetryAfterSeconds(probe.getNanosToWaitForRefill());
            return new RateLimitDecision(probe.isConsumed(), policy, identity.type(),
                    policy.capacity(), probe.getRemainingTokens(), retryAfterSeconds);
        } catch (RuntimeException e) {
            throw new RateLimitUnavailableException(e);
        }
    }

    static String bucketKey(RateLimitPolicy policy, RateLimitIdentity identity) {
        return KEY_PREFIX + policy.name().toLowerCase() + ":" + identity.keyPart();
    }

    /** 나노초를 올림한 초로. 최소 1초 */
    static long toRetryAfterSeconds(long nanosToWait) {
        return Math.max(1, (long) Math.ceil(nanosToWait / 1_000_000_000.0));
    }

    private static BucketConfiguration configurationFor(RateLimitPolicy policy) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(policy.capacity())
                .refillGreedy(policy.capacity(), policy.period())
                .build();
        return BucketConfiguration.builder().addLimit(limit).build();
    }

    private ProxyManager<byte[]> proxyManager() {
        ProxyManager<byte[]> current = proxyManager;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (proxyManager == null) {
                proxyManager = proxyManagerSupplier.get();
            }
            return proxyManager;
        }
    }
}
