package com.ssafy.nyamnyam.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class VerifyEmailRequest {

    @NotBlank(message = "인증 토큰이 누락되었습니다.")
    private String token;

    public VerifyEmailRequest(String token) {
        this.token = token;
    }
}
