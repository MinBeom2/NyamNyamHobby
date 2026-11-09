package com.ssafy.nyamnyam.domain.report;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 신고 설정(app.report.*).
 *
 * @param autoHideThreshold    서로 다른 사용자의 처리 전 신고가 이 수에 닿으면 바로 숨김
 * @param maxTargetsPerDigest  한 번에 Mattermost로 보낼 대상 수(넘치면 다음 시간에 이어서)
 * @param notifyImmediately    true면 신고가 들어오자마자 보낸다(로컬 확인용). 서버는 false로 매시 정각에만 보낸다
 * @param mattermostWebhookUrl 신고 채널 수신 웹훅. 비면 알림을 보내지 않는다
 * @param actionSecret         처리 버튼 HMAC 서명 키. 비면 버튼 없이 알림만 보낸다
 * @param publicBaseUrl        Mattermost가 버튼 클릭을 보낼 백엔드 주소(https://k15d104.p.ssafy.io)
 * @param frontendUrl          알림에 넣을 게시글 링크의 앞부분
 */
@ConfigurationProperties(prefix = "app.report")
public record ReportProperties(
        @DefaultValue("3") int autoHideThreshold,
        @DefaultValue("30") int maxTargetsPerDigest,
        @DefaultValue("false") boolean notifyImmediately,
        @DefaultValue("") String mattermostWebhookUrl,
        @DefaultValue("") String actionSecret,
        @DefaultValue("http://localhost:8080") String publicBaseUrl,
        @DefaultValue("http://localhost:5173") String frontendUrl
) {
    public boolean notifyEnabled() {
        return mattermostWebhookUrl != null && !mattermostWebhookUrl.isBlank();
    }

    public boolean actionsEnabled() {
        return actionSecret != null && !actionSecret.isBlank();
    }
}
