package com.ssafy.nyamnyam.domain.exploration.dto.request;

import com.ssafy.nyamnyam.domain.exploration.entity.ReactionType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReactionRequest {

    @NotNull(message = "반응 유형은 필수입니다.")
    private ReactionType type;

    private Long sessionId;
}
