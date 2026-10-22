package com.ssafy.nyamnyam.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CheckEmailResponse {
    private String email;
    private boolean available;
}
