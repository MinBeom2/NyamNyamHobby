package com.ssafy.nyamnyam.global.ratelimit;

import com.ssafy.nyamnyam.global.common.ApiResponse;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import com.ssafy.nyamnyam.global.error.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * /api/v1/** 요청마다 정책을 골라 토큰 하나를 쓴다. JWT 필터 바로 뒤에 둬서 로그인 사용자를 알 수 있다.
 * <ul>
 *   <li>허용: X-RateLimit-Limit·X-RateLimit-Remaining 헤더를 달고 다음 필터로</li>
 *   <li>초과: 429 + Retry-After 헤더 + 공통 오류 본문(retryAfterSeconds)</li>
 *   <li>Redis 장애: 메일·GMS처럼 돈이 드는 정책은 503, 나머지는 통과(fail-open)</li>
 * </ul>
 * 필터에서 직접 응답을 쓰는 이유: 여기서 던진 예외는 GlobalExceptionHandler(@RestControllerAdvice)까지 가지 않는다.
 */
@Slf4j
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    public static final String LIMIT_HEADER = "X-RateLimit-Limit";
    public static final String REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final String API_PREFIX = "/api/v1/";
    private static final String IMAGE_PREFIX = "/api/v1/uploads/images/";

    private final RateLimitProperties properties;
    private final ClientIdentityResolver identityResolver;
    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    /** 꺼져 있거나, API가 아니거나, CORS 사전 요청이거나, 업로드 이미지 파일 조회(한 화면에 여러 장)면 세지 않는다 */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !properties.enabled()
                || !uri.startsWith(API_PREFIX)
                || "OPTIONS".equalsIgnoreCase(request.getMethod())
                || ("GET".equalsIgnoreCase(request.getMethod()) && uri.startsWith(IMAGE_PREFIX));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        RateLimitPolicy policy = RateLimitPolicy.resolve(request.getMethod(), request.getRequestURI());
        RateLimitIdentity identity = identityResolver.resolve(request, policy);

        RateLimitDecision decision;
        try {
            decision = rateLimitService.consume(policy, identity);
        } catch (RateLimitUnavailableException e) {
            if (policy.rejectWhenUnavailable()) {
                log.error("Rate limit store unavailable, rejecting: policy={}, uri={}", policy, request.getRequestURI(), e);
                writeError(response, ErrorCode.RATE_LIMIT_UNAVAILABLE, null);
                return;
            }
            log.warn("Rate limit store unavailable, allowing: policy={}, uri={}, cause={}",
                    policy, request.getRequestURI(), e.getCause() != null ? e.getCause().toString() : "-");
            filterChain.doFilter(request, response);
            return;
        }

        response.setHeader(LIMIT_HEADER, Long.toString(decision.limit()));
        response.setHeader(REMAINING_HEADER, Long.toString(decision.remaining()));
        if (!decision.allowed()) {
            log.warn("Rate limit exceeded: policy={}, identity={}, uri={}",
                    policy, identity.keyPart(), request.getRequestURI());
            response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(decision.retryAfterSeconds()));
            writeError(response, ErrorCode.RATE_LIMIT_EXCEEDED, (int) decision.retryAfterSeconds());
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode, Integer retryAfterSeconds) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiResponse<Void> body = ApiResponse.error(ErrorResponse.of(errorCode, retryAfterSeconds));
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
