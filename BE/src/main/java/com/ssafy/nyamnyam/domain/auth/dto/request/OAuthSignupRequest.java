package com.ssafy.nyamnyam.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OAuthSignupRequest {

    @NotBlank(message = "임시 토큰이 누락되었습니다.")
    private String tempToken;

    @NotBlank(message = "닉네임을 입력해 주세요.")
    @Pattern(
            regexp = "^[가-힣a-zA-Z0-9]{2,12}$",
            message = "닉네임은 2~12자의 한글, 영문, 숫자만 사용 가능합니다."
    )
    private String nickname;

    public OAuthSignupRequest(String tempToken, String nickname) {
        this.tempToken = tempToken;
        this.nickname = nickname;
    }
}
