package com.ssafy.nyamnyam.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetConfirmRequest {

    @NotBlank(message = "인증 토큰이 누락되었습니다.")
    private String token;

    @NotBlank(message = "새 비밀번호를 입력해 주세요.")
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자로 입력해 주세요.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d|.*[^A-Za-z0-9])|(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,20}$",
            message = "비밀번호는 영문, 숫자, 특수문자 중 2종 이상을 조합해야 합니다."
    )
    private String newPassword;
}
