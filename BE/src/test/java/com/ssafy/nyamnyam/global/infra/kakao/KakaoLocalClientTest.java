package com.ssafy.nyamnyam.global.infra.kakao;

import com.ssafy.nyamnyam.global.infra.publicdata.PublicDataApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KakaoLocalClientTest {

    @Test
    @DisplayName("API 키가 비어있으면 isConfigured가 false를 반환하고 호출 시 예외를 던진다")
    void unconfiguredClientThrowsException() {
        KakaoLocalClient client = new KakaoLocalClient("https://dapi.kakao.com", "");

        assertThat(client.isConfigured()).isFalse();
        assertThatThrownBy(() -> client.searchByKeyword("마포구 클라이밍", 1, 15))
                .isInstanceOf(PublicDataApiException.class)
                .hasMessageContaining("Kakao Local REST API key is not configured");
    }

    @Test
    @DisplayName("API 키가 제공되면 isConfigured가 true를 반환한다")
    void configuredClient() {
        KakaoLocalClient client = new KakaoLocalClient("https://dapi.kakao.com", "dummy-api-key");
        assertThat(client.isConfigured()).isTrue();
    }
}

