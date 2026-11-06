package com.ssafy.nyamnyam.global.ratelimit;

/**
 * 토큰 하나를 쓰려고 한 결과.
 *
 * @param limit             버킷 용량(X-RateLimit-Limit)
 * @param remaining         남은 토큰 수(X-RateLimit-Remaining)
 * @param retryAfterSeconds 거절됐을 때 토큰 하나가 다시 찰 때까지 남은 초(Retry-After). 허용이면 0
 */
public record RateLimitDecision(
        boolean allowed,
        RateLimitPolicy policy,
        RateLimitIdentity.Type identityType,
        long limit,
        long remaining,
        long retryAfterSeconds
) {
}
