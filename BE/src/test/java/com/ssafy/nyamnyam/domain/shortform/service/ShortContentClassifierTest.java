package com.ssafy.nyamnyam.domain.shortform.service;

import com.ssafy.nyamnyam.domain.shortform.service.ShortContentClassifier.Verdict;
import com.ssafy.nyamnyam.global.infra.gms.GmsClient;
import com.ssafy.nyamnyam.global.infra.youtube.YoutubeVideo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ShortContentClassifierTest {

    @Mock
    private GmsClient gmsClient;

    private ShortContentClassifier classifier;

    @BeforeEach
    void setUp() {
        ShortCollectProperties properties = new ShortCollectProperties(true, 2, 40, 3, 1000, 180, 9000, 0.7, 0.9, 2, 7, 30, 5);
        classifier = new ShortContentClassifier(gmsClient, JsonMapper.builder().build(), properties);
    }

    @Test
    @DisplayName("GPT JSON 응답을 영상별 판별 결과로 읽고, 형식이 어긋난 항목은 건너뜀")
    void parse() {
        Map<String, Verdict> result = classifier.parse("""
                {"results":[
                  {"videoId":"a","isHobbyContent":true,"confidence":0.92},
                  {"videoId":"b","isHobbyContent":false,"confidence":1.4},
                  {"videoId":"c","confidence":0.5},
                  {"videoId":"d","isHobbyContent":true,"confidence":0.8,"extra":"ignored"}
                ]}""");

        assertThat(result).containsOnlyKeys("a", "b", "d");
        assertThat(result.get("a")).isEqualTo(new Verdict(true, 0.92));
        assertThat(result.get("b")).isEqualTo(new Verdict(false, 1.0));
    }

    @Test
    @DisplayName("JSON이 아니면 빈 결과(→ NEEDS_REVIEW)")
    void parseInvalid() {
        assertThat(classifier.parse("죄송합니다, 판단할 수 없습니다")).isEmpty();
    }

    @Test
    @DisplayName("GMS 키가 없으면 호출하지 않음")
    void notConfigured() {
        given(gmsClient.isConfigured()).willReturn(false);

        assertThat(classifier.classify("클라이밍", List.of(ShortFilterTest.ok("a", "c")))).isEmpty();
        verify(gmsClient, never()).completeJson(anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("묶음 크기만큼 나눠 부르고, 보내지 않은 영상 ID는 결과에서 버림")
    void classifyInChunks() {
        given(gmsClient.isConfigured()).willReturn(true);
        given(gmsClient.completeJson(anyString(), anyString(), anyInt())).willReturn(
                Optional.of("""
                        {"results":[{"videoId":"a","isHobbyContent":true,"confidence":0.9},
                                    {"videoId":"zzz","isHobbyContent":true,"confidence":0.9}]}"""),
                Optional.empty());

        Map<String, Verdict> result = classifier.classify("클라이밍",
                List.of(ShortFilterTest.ok("a", "c"), ShortFilterTest.ok("b", "c"), ShortFilterTest.ok("x", "c")));

        assertThat(result).containsOnlyKeys("a");
        verify(gmsClient, times(2)).completeJson(anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("프롬프트에 취미와 영상 ID·제목이 들어가고 줄바꿈은 한 줄로 정리")
    void prompt() {
        YoutubeVideo video = new YoutubeVideo("vid1", "c", "채널", "볼더링\n첫 도전", "설명\n\n여러 줄", null,
                50, null, "public", true, null, null, 5000);

        String prompt = ShortContentClassifier.buildUserPrompt("클라이밍", List.of(video));

        assertThat(prompt).contains("취미: 클라이밍", "videoId=vid1", "볼더링 첫 도전", "설명 여러 줄", "\"results\"");
    }
}
