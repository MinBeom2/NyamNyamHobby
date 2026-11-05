package com.ssafy.nyamnyam.global.ratelimit;

public interface RateLimitService {

    /**
     * 정책·기준에 해당하는 버킷에서 토큰 하나를 쓴다.
     *
     * @throws RateLimitUnavailableException 저장소(Redis)를 쓸 수 없을 때
     */
    RateLimitDecision consume(RateLimitPolicy policy, RateLimitIdentity identity);
}
