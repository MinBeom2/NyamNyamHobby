package com.ssafy.nyamnyam.global.ratelimit;

import java.util.Locale;

/** 버킷을 나누는 기준. 로그인 사용자는 사용자 ID, 그 밖에는 IP */
public record RateLimitIdentity(Type type, String value) {

    public enum Type {
        USER,
        IP
    }

    public static RateLimitIdentity user(Long userId) {
        return new RateLimitIdentity(Type.USER, String.valueOf(userId));
    }

    public static RateLimitIdentity ip(String ip) {
        return new RateLimitIdentity(Type.IP, ip);
    }

    /** Redis 키 일부(예: user:15, ip:203.0.113.10) */
    public String keyPart() {
        return type.name().toLowerCase(Locale.ROOT) + ":" + value;
    }
}
