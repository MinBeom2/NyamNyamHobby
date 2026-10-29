package com.ssafy.nyamnyam.domain.user.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateProfileRequest {

    @Pattern(
            regexp = "^[가-힣a-zA-Z0-9]{2,12}$",
            message = "닉네임은 2~12자의 한글, 영문, 숫자만 사용 가능합니다."
    )
    private String nickname;

    @Size(max = 500, message = "프로필 이미지 URL은 최대 500자까지 가능합니다.")
    private String profileImageUrl;

    public UpdateProfileRequest(String nickname, String profileImageUrl) {
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }
}

