package com.ssafy.nyamnyam.global.infra.mattermost;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/** Mattermost 수신 웹훅 전송. 실패해도 예외를 던지지 않고 false를 돌려준다(호출한 쪽이 다음에 다시 보냄) */
@Slf4j
@Component
public class MattermostWebhookClient {

    private final RestClient restClient;

    public MattermostWebhookClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public boolean send(String webhookUrl, MattermostMessage message) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return false;
        }
        try {
            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(message)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (RuntimeException e) {
            // 웹훅 URL에는 비밀 토큰이 들어 있으므로 로그에 남기지 않는다
            log.warn("Mattermost webhook failed: {}", e.getClass().getSimpleName());
            return false;
        }
    }
}
