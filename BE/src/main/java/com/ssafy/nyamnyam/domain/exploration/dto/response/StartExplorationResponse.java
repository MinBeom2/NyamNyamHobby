package com.ssafy.nyamnyam.domain.exploration.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StartExplorationResponse {

    private Long sessionId;
    private Integer maxCount;
    private Integer viewedCount;

    @JsonProperty("short")
    private ShortDetailResponse shortItem;
}
