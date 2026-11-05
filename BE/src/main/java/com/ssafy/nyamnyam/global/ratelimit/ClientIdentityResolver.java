package com.ssafy.nyamnyam.global.ratelimit;

import com.ssafy.nyamnyam.global.security.UserPrincipal;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * 요청을 어느 버킷으로 셀지 정한다. 로그인 사용자는 사용자 ID, 그 밖에는 클라이언트 IP.
 *
 * <h3>클라이언트 IP</h3>
 * server.forward-headers-strategy=framework 이면 ForwardedHeaderFilter가 요청을 감싸서 getRemoteAddr()가
 * X-Forwarded-For의 첫 값(클라이언트가 마음대로 넣을 수 있는 값)을 돌려준다. 그래서
 * <ol>
 *   <li>감싸기 전 원래 요청의 접속 주소(peer)를 꺼내고</li>
 *   <li>peer가 신뢰하는 프록시 대역일 때만 프록시가 덮어쓴 헤더(X-Real-IP)를 믿는다.</li>
 * </ol>
 */
public class ClientIdentityResolver {

    private final String clientIpHeader;
    private final List<IpAddressMatcher> trustedProxies;

    public ClientIdentityResolver(RateLimitProperties properties) {
        this.clientIpHeader = properties.clientIpHeader();
        this.trustedProxies = properties.trustedProxyCidrs().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .map(IpAddressMatcher::new)
                .toList();
    }

    public RateLimitIdentity resolve(HttpServletRequest request, RateLimitPolicy policy) {
        if (!policy.ipOnly()) {
            Long userId = currentUserId();
            if (userId != null) {
                return RateLimitIdentity.user(userId);
            }
        }
        return RateLimitIdentity.ip(clientIp(request));
    }

    String clientIp(HttpServletRequest request) {
        String peer = originalRemoteAddr(request);
        if (isTrustedProxy(peer)) {
            String forwarded = lastValue(request.getHeader(clientIpHeader));
            String ip = normalizeIp(forwarded);
            if (ip != null) {
                return ip;
            }
        }
        return peer;
    }

    private static Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUserId();
        }
        return null;
    }

    /** 필터들이 감싼 요청을 벗겨 서블릿 컨테이너가 준 원래 요청의 접속 주소 */
    static String originalRemoteAddr(ServletRequest request) {
        ServletRequest current = request;
        while (current instanceof ServletRequestWrapper wrapper) {
            current = wrapper.getRequest();
        }
        return current.getRemoteAddr();
    }

    /**
     * 값이 여러 개(a, b)면 마지막 값. 프록시가 한 대일 때 마지막 값이 그 프록시가 직접 본 주소다.
     * X-Real-IP는 값이 하나라 그대로 쓰인다.
     */
    private static String lastValue(String header) {
        if (!StringUtils.hasText(header)) {
            return null;
        }
        String[] values = header.split(",");
        return values[values.length - 1].trim();
    }

    private boolean isTrustedProxy(String address) {
        return normalizeIp(address) != null && trustedProxies.stream().anyMatch(m -> m.matches(address));
    }

    /** 숫자 IP(IPv4·IPv6)만 받는다. 호스트 이름은 DNS 조회가 일어나므로 거절 */
    static String normalizeIp(String address) {
        if (!StringUtils.hasText(address) || !isNumericIp(address)) {
            return null;
        }
        try {
            return InetAddress.getByName(address).getHostAddress();
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private static boolean isNumericIp(String address) {
        if (address.indexOf(':') >= 0) {
            return address.matches("[0-9a-fA-F:.]+");
        }
        String[] octets = address.split("\\.", -1);
        if (octets.length != 4) {
            return false;
        }
        for (String octet : octets) {
            if (octet.isEmpty() || octet.length() > 3 || !octet.chars().allMatch(Character::isDigit)
                    || Integer.parseInt(octet) > 255) {
                return false;
            }
        }
        return true;
    }
}
