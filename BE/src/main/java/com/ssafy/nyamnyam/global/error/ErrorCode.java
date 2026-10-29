package com.ssafy.nyamnyam.global.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 400 Bad Request
    COMMON_INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_INVALID_INPUT", "입력값을 확인해 주세요."),
    AUTH_LINK_INVALID(HttpStatus.BAD_REQUEST, "AUTH_LINK_INVALID", "인증 링크가 만료되었거나 올바르지 않습니다."),
    AUTH_RESEND_COOLDOWN(HttpStatus.BAD_REQUEST, "AUTH_RESEND_COOLDOWN", "인증 메일은 60초에 1회만 재발송할 수 있습니다."),
    AUTH_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH_PASSWORD_MISMATCH", "현재 비밀번호가 일치하지 않습니다."),

    // 401 Unauthorized
    COMMON_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON_UNAUTHORIZED", "인증이 필요합니다."),
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않아요."),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_TOKEN_EXPIRED", "인증 토큰이 만료되었습니다. 다시 로그인해 주세요."),
    AUTH_REFRESH_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_REFRESH_INVALID", "유효하지 않은 Refresh Token입니다. 다시 로그인해 주세요."),

    // 403 Forbidden
    COMMON_FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON_FORBIDDEN", "권한이 없습니다."),
    AUTH_ACCOUNT_LOCKED(HttpStatus.FORBIDDEN, "AUTH_ACCOUNT_LOCKED", "비밀번호 5회 연속 오류로 계정이 5분간 잠겼습니다."),
    AUTH_EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "AUTH_EMAIL_NOT_VERIFIED", "이메일 인증이 완료되지 않았습니다. 인증 메일을 확인해 주세요."),

    // 404 Not Found
    HOBBY_RECOMMENDATION_UNAVAILABLE(HttpStatus.NOT_FOUND, "HOBBY_RECOMMENDATION_UNAVAILABLE", "추천 가능한 공개 취미와 태그 정보를 찾을 수 없습니다."),
    COMMON_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_NOT_FOUND", "요청하신 리소스를 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    HOBBY_NOT_FOUND(HttpStatus.NOT_FOUND, "HOBBY_NOT_FOUND", "취미를 찾을 수 없습니다."),
    REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "REGION_NOT_FOUND", "지역을 찾을 수 없습니다."),

    // 409 Conflict
    SHORT_SESSION_ENDED(HttpStatus.CONFLICT, "SHORT_SESSION_ENDED", "변경할 수 없는 탐색 세션입니다."),
    SHORT_NOT_ACTIVE(HttpStatus.CONFLICT, "SHORT_NOT_ACTIVE", "현재 반응할 수 없는 숏폼입니다."),
    AUTH_EMAIL_DUPLICATED(HttpStatus.CONFLICT, "AUTH_EMAIL_DUPLICATED", "이미 사용 중인 이메일입니다."),
    AUTH_NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "AUTH_NICKNAME_DUPLICATED", "이미 사용 중인 닉네임입니다."),
    AUTH_SOCIAL_ACCOUNT(HttpStatus.CONFLICT, "AUTH_SOCIAL_ACCOUNT", "다른 방법으로 가입된 계정입니다."),

    // 500 Internal Server Error
    COMMON_INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
