package com.ssafy.nyamnyam.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CheckNicknameResponse {
    private String nickname;
    private boolean available;
}
