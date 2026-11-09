package com.ssafy.nyamnyam.domain.report.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * 버튼 클릭 응답. update는 원래 메시지를 바꾸고(버튼을 없애고 처리 결과 표시), ephemeral_text는 누른 사람에게만 보인다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MattermostActionResponse(
        Update update,
        @JsonProperty("ephemeral_text") String ephemeralText
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Update(String message, Map<String, Object> props) {
    }
}
