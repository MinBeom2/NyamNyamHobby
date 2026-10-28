package com.ssafy.nyamnyam.domain.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequest {

    @NotBlank(message = "이메일을 입력해 주세요.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Size(max = 100, message = "이메일은 최대 100자까지 가능합니다.")
    private String email;

    @NotBlank(message = "비밀번호를 입력해 주세요.")
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자로 입력해 주세요.")
    @Pattern(
            regexp = "^(?:(?=.*[A-Za-z])(?=.*\\d)|(?=.*[A-Za-z])(?=.*[^A-Za-z0-9])|(?=.*\\d)(?=.*[^A-Za-z0-9])).{8,20}$",
            message = "비밀번호는 영문, 숫자, 특수문자 중 2종 이상을 조합해야 합니다."
    )

    private String password;

    @NotBlank(message = "닉네임을 입력해 주세요.")
    @Pattern(
            regexp = "^[가-힣a-zA-Z0-9]{2,12}$",
            message = "닉네임은 2~12자의 한글, 영문, 숫자만 사용 가능합니다."
    )
    private String nickname;

    public SignupRequest(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
    }
}
