package com.ssafy.nyamnyam.domain.user.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WithdrawRequest {

    private String password;

    public WithdrawRequest(String password) {
        this.password = password;
    }
}

