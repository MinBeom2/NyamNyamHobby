package com.ssafy.nyamnyam.global.infra.gms;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class GmsClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public GmsClient(
            @Value("${gms.base-url:https://api.openai.com}") String baseUrl,
            @Value("${gms.api-key:}") String apiKey,
            @Value("${gms.model:gpt-4o}") String model
    ) {
        this.apiKey = apiKey;
        this.model = model != null && !model.isBlank() ? model : "gpt-4o";

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * 추천 이유 1문장(60자 이내) 생성
     */
    public String generateRecommendationReason(String hobbyName, String subjectTag, String gainTag, String userReactionSummary) {
        if (apiKey == null || apiKey.isBlank()) {
            log.info("GMS API Key is not configured. Using fallback recommendation reason.");
            return createFallbackReason(hobbyName, userReactionSummary);
        }

        try {
            String prompt = String.format(
                    "취미명: %s\n소재: %s, 얻는 가치: %s\n사용자 반응 요약: %s\n\n위 정보를 바탕으로 사용자에게 이 취미를 추천하는 이유를 60자 이내의 친근하고 매력적인 한국어 한 문장으로 작성해줘.",
                    hobbyName,
                    subjectTag != null ? subjectTag : "활동",
                    gainTag != null ? gainTag : "즐거움",
                    userReactionSummary != null ? userReactionSummary : "영상 시청"
            );

            ChatRequest request = new ChatRequest(
                    model,
                    List.of(
                            new ChatMessage("system", "너는 취미 추천 서비스 '냠냠취미'의 추천 AI 어시스턴트야. 항상 60자 이내의 자연스러운 한국어 한 문장으로 답변해."),
                            new ChatMessage("user", prompt)
                    ),
                    100,
                    0.7
            );

            ChatResponse response = restClient.post()
                    .uri("/v1/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ChatResponse.class);

            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                String content = response.getChoices().get(0).getMessage().getContent().trim();
                // 따옴표나 불필요한 공백 제거
                content = content.replaceAll("^\"|\"$", "");
                if (content.length() > 60) {
                    content = content.substring(0, 57) + "...";
                }
                return content;
            }
        } catch (Exception e) {
            log.warn("Failed to call GMS API: {}. Using fallback reason.", e.getMessage());
        }

        return createFallbackReason(hobbyName, userReactionSummary);
    }

    private String createFallbackReason(String hobbyName, String userReactionSummary) {
        if (userReactionSummary != null && userReactionSummary.contains("좋아요")) {
            return String.format("%s 관련 영상에 좋아요를 남겨주셔서 맞춤으로 추천해 드려요!", hobbyName);
        }
        return String.format("관심 있게 보신 영상을 바탕으로 %s 취미를 추천해 드려요!", hobbyName);
    }

    @Getter
    @NoArgsConstructor
    public static class ChatRequest {
        private String model;
        private List<ChatMessage> messages;
        @JsonProperty("max_tokens")
        private Integer maxTokens;
        private Double temperature;

        public ChatRequest(String model, List<ChatMessage> messages, Integer maxTokens, Double temperature) {
            this.model = model;
            this.messages = messages;
            this.maxTokens = maxTokens;
            this.temperature = temperature;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class ChatMessage {
        private String role;
        private String content;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class ChatResponse {
        private List<Choice> choices;

        @Getter
        @NoArgsConstructor
        public static class Choice {
            private ChatMessage message;
        }
    }
}
