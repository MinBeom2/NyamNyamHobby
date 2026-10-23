package com.ssafy.nyamnyam.domain.exploration.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NextShortResponse {

    private Boolean finished;
    private Integer viewedCount;

    @JsonProperty("short")
    private ShortDetailResponse shortItem;
}
