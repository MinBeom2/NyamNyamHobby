package com.ssafy.nyamnyam.global.ratelimit;

/** 레이트 리밋 저장소(Redis)를 쓸 수 없음. 필터가 정책에 따라 막거나 통과시킨다 */
public class RateLimitUnavailableException extends RuntimeException {

    public RateLimitUnavailableException(Throwable cause) {
        super("레이트 리밋 저장소를 사용할 수 없습니다.", cause);
    }
}
