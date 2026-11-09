package com.ssafy.nyamnyam.domain.report.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Mattermost가 버튼 클릭 때 보내는 요청(필요한 값만). context는 버튼을 만들 때 넣어 둔 값이 그대로 돌아온다:
 * decision, targetType, targetId, sig
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MattermostActionRequest(
        @JsonProperty("user_id") String userId,
        @JsonProperty("user_name") String userName,
        @JsonProperty("post_id") String postId,
        Map<String, Object> context
) {
}
