package com.ssafy.nyamnyam.domain.exploration.dto.response;

import com.ssafy.nyamnyam.domain.exploration.entity.ReactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReactionResponse {
    private Long shortId;
    private ReactionType type;
}
