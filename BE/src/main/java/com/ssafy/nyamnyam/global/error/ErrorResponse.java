package com.ssafy.nyamnyam.global.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.validation.BindingResult;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@JsonInclude(JsonInclude.Include.ALWAYS)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ErrorResponse {

    private String code;
    private String message;
    private List<FieldErrorDetail> fields;
    private Integer retryAfterSeconds;

    private ErrorResponse(ErrorCode errorCode, List<FieldErrorDetail> fields, Integer retryAfterSeconds) {
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
        this.fields = fields != null ? fields : Collections.emptyList();
        this.retryAfterSeconds = retryAfterSeconds;
    }

    private ErrorResponse(ErrorCode errorCode, String customMessage, List<FieldErrorDetail> fields, Integer retryAfterSeconds) {
        this.code = errorCode.getCode();
        this.message = customMessage != null ? customMessage : errorCode.getMessage();
        this.fields = fields != null ? fields : Collections.emptyList();
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode, Collections.emptyList(), null);
    }

    public static ErrorResponse of(ErrorCode errorCode, Integer retryAfterSeconds) {
        return new ErrorResponse(errorCode, Collections.emptyList(), retryAfterSeconds);
    }

    public static ErrorResponse of(ErrorCode errorCode, String customMessage) {
        return new ErrorResponse(errorCode, customMessage, Collections.emptyList(), null);
    }

    public static ErrorResponse of(ErrorCode errorCode, BindingResult bindingResult) {
        return new ErrorResponse(errorCode, FieldErrorDetail.of(bindingResult), null);
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class FieldErrorDetail {
        private String field;
        private String reason;

        public FieldErrorDetail(String field, String reason) {
            this.field = field;
            this.reason = reason;
        }

        public static List<FieldErrorDetail> of(BindingResult bindingResult) {
            if (bindingResult == null || bindingResult.getFieldErrors().isEmpty()) {
                return Collections.emptyList();
            }
            return bindingResult.getFieldErrors().stream()
                    .map(error -> new FieldErrorDetail(
                            error.getField(),
                            error.getDefaultMessage() != null ? error.getDefaultMessage() : ""
                    ))
                    .collect(Collectors.toList());
        }
    }
}
