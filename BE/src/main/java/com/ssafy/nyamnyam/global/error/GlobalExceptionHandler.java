package com.ssafy.nyamnyam.global.error;

import com.ssafy.nyamnyam.global.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableMessage(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(
                ErrorResponse.of(ErrorCode.COMMON_INVALID_INPUT)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        log.warn("Validation failed: {}", e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.COMMON_INVALID_INPUT, e.getBindingResult());
        return ResponseEntity
                .status(ErrorCode.COMMON_INVALID_INPUT.getHttpStatus())
                .body(ApiResponse.error(errorResponse));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException e) {
        log.warn("Bind failed: {}", e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.COMMON_INVALID_INPUT, e.getBindingResult());
        return ResponseEntity
                .status(ErrorCode.COMMON_INVALID_INPUT.getHttpStatus())
                .body(ApiResponse.error(errorResponse));
    }

    /** 쿼리·경로 값 형식 오류(예: type=ABC, postId=abc) → 400 */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequestParameter(Exception e) {
        log.warn("Bad request parameter: {}", e.getMessage());
        return ResponseEntity
                .status(ErrorCode.COMMON_INVALID_INPUT.getHttpStatus())
                .body(ApiResponse.error(ErrorResponse.of(ErrorCode.COMMON_INVALID_INPUT)));
    }

    /** multipart 최대 크기(spring.servlet.multipart.max-file-size) 초과 → 413 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return ResponseEntity
                .status(ErrorCode.UPLOAD_TOO_LARGE.getHttpStatus())
                .body(ApiResponse.error(ErrorResponse.of(ErrorCode.UPLOAD_TOO_LARGE)));
    }

    /** 없는 정적 파일(업로드 이미지 등) → 404 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity
                .status(ErrorCode.COMMON_NOT_FOUND.getHttpStatus())
                .body(ApiResponse.error(ErrorResponse.of(ErrorCode.COMMON_NOT_FOUND)));
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("CustomException [{}]: {}", errorCode.getCode(), e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(errorCode, e.getMessage());
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorResponse));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException e) {
        log.warn("AuthenticationException: {}", e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.COMMON_UNAUTHORIZED);
        return ResponseEntity
                .status(ErrorCode.COMMON_UNAUTHORIZED.getHttpStatus())
                .body(ApiResponse.error(errorResponse));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("AccessDeniedException: {}", e.getMessage());
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.COMMON_FORBIDDEN);
        return ResponseEntity
                .status(ErrorCode.COMMON_FORBIDDEN.getHttpStatus())
                .body(ApiResponse.error(errorResponse));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception e) {
        log.error("Unhandled Exception: ", e);
        ErrorResponse errorResponse = ErrorResponse.of(ErrorCode.COMMON_INTERNAL_SERVER_ERROR);
        return ResponseEntity
                .status(ErrorCode.COMMON_INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ApiResponse.error(errorResponse));
    }
}
