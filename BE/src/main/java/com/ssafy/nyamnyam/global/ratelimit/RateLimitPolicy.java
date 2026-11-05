package com.ssafy.nyamnyam.global.ratelimit;

import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * API 그룹별 토큰 버킷 정책. 버킷은 capacity개까지 쌓이고, period 동안 capacity개가 고르게 다시 찬다.
 * <ul>
 *   <li>ipOnly: 로그인 여부와 상관없이 IP로 센다. 인증 API는 계정을 바꿔 가며 시도할 수 있기 때문이다.</li>
 *   <li>rejectWhenUnavailable: Redis 장애 시 막을지 여부. 돈이 드는 요청(메일 발송·GMS 호출)만 막고 나머지는 통과시킨다.</li>
 * </ul>
 * 구체적인 규칙을 먼저 검사해야 하므로 {@link #resolve(String, String)}의 순서가 중요하다.
 */
public enum RateLimitPolicy {

    /** 메일 발송(회원가입·인증 메일 재발송·비밀번호 재설정). 남의 메일함으로 메일 폭탄이 가지 않게 한다 */
    AUTH_MAIL(10, Duration.ofMinutes(10), true, true),
    /** 그 밖의 인증 API(로그인·중복 확인·토큰 재발급). 계정 잠금은 계정 단위라 IP 단위로 한 번 더 막는다 */
    AUTH(60, Duration.ofMinutes(1), true, false),
    /** 추천 생성. 추천 사유를 GMS(유료 LLM)로 만든다 */
    AI(10, Duration.ofMinutes(1), false, true),
    /** 이미지 업로드. 서버 디스크를 쓴다 */
    UPLOAD(30, Duration.ofMinutes(10), false, false),
    /** 게시글·댓글 작성. 도배를 막는다 */
    CONTENT(10, Duration.ofMinutes(1), false, false),
    /** 그 밖의 변경 요청 */
    WRITE(60, Duration.ofMinutes(1), false, false),
    /** 조회. 비로그인은 IP로 세므로 과도한 수집(크롤링)을 늦춘다. 공용 와이파이(한 IP에 여러 명)를 고려해 넉넉하게 둔다 */
    READ(300, Duration.ofMinutes(1), false, false);

    private static final Set<String> MAIL_SENDING_PATHS = Set.of(
            "/api/v1/auth/signup",
            "/api/v1/auth/email/resend",
            "/api/v1/auth/password/reset-request");
    private static final Pattern COMMENT_CREATE = Pattern.compile("^/api/v1/posts/[^/]+/comments$");
    private static final Set<String> READ_METHODS = Set.of("GET", "HEAD");

    private final long capacity;
    private final Duration period;
    private final boolean ipOnly;
    private final boolean rejectWhenUnavailable;

    RateLimitPolicy(long capacity, Duration period, boolean ipOnly, boolean rejectWhenUnavailable) {
        this.capacity = capacity;
        this.period = period;
        this.ipOnly = ipOnly;
        this.rejectWhenUnavailable = rejectWhenUnavailable;
    }

    public long capacity() {
        return capacity;
    }

    public Duration period() {
        return period;
    }

    public boolean ipOnly() {
        return ipOnly;
    }

    public boolean rejectWhenUnavailable() {
        return rejectWhenUnavailable;
    }

    /** 요청 메서드와 경로로 정책을 고른다. 모르는 메서드(PROPFIND 등)는 쓰기로 본다 */
    public static RateLimitPolicy resolve(String method, String uri) {
        String m = method == null ? "" : method.toUpperCase(Locale.ROOT);
        boolean post = "POST".equals(m);

        if (uri.startsWith("/api/v1/auth/")) {
            return post && MAIL_SENDING_PATHS.contains(uri) ? AUTH_MAIL : AUTH;
        }
        if (post && "/api/v1/recommendations".equals(uri)) {
            return AI;
        }
        if (post && "/api/v1/uploads/images".equals(uri)) {
            return UPLOAD;
        }
        if (post && ("/api/v1/posts".equals(uri) || COMMENT_CREATE.matcher(uri).matches())) {
            return CONTENT;
        }
        return READ_METHODS.contains(m) ? READ : WRITE;
    }
}
