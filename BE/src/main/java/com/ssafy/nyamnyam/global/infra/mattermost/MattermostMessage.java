package com.ssafy.nyamnyam.global.infra.mattermost;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * Mattermost 수신 웹훅 메시지(텍스트 + 메시지 첨부). 첨부의 actions가 버튼이고,
 * 버튼을 누르면 Mattermost 서버가 integration.url 로 context를 담아 POST한다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MattermostMessage(String text, List<Attachment> attachments) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Attachment(
            String fallback,
            String color,
            String title,
            @JsonProperty("title_link") String titleLink,
            String text,
            List<Field> fields,
            List<Action> actions
    ) {
    }

    public record Field(String title, String value, @JsonProperty("short") boolean isShort) {
    }

    /** id는 영문·숫자만(Mattermost 제약). style: default·primary·success·good·warning·danger */
    public record Action(String id, String name, String style, Integration integration) {
    }

    public record Integration(String url, Map<String, Object> context) {
    }
}
