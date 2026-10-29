package com.ssafy.nyamnyam.domain.exploration.dto.request;

import com.ssafy.nyamnyam.domain.exploration.entity.ReactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReactionRequest {

    @NotNull(message = "반응 유형은 필수입니다.")
    private ReactionType type;

    @NotNull(message = "탐색 세션은 필수입니다.")
    @Positive(message = "탐색 세션 ID는 양수여야 합니다.")
    private Long sessionId;
}
