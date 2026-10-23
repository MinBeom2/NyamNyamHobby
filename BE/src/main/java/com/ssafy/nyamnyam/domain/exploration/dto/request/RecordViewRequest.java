package com.ssafy.nyamnyam.domain.exploration.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RecordViewRequest {

    @NotNull(message = "shortId는 필수입니다.")
    private Long shortId;

    private Integer watchedSeconds;

    private Boolean playFailed;
}
