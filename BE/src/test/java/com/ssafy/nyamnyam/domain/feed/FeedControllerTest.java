package com.ssafy.nyamnyam.domain.feed;

import com.ssafy.nyamnyam.domain.feed.controller.FeedController;
import com.ssafy.nyamnyam.domain.feed.dto.response.FeedItemResponse;
import com.ssafy.nyamnyam.domain.feed.entity.FeedType;
import com.ssafy.nyamnyam.domain.feed.service.FeedService;
import com.ssafy.nyamnyam.global.common.PageResponse;
import com.ssafy.nyamnyam.global.security.SecurityConfig;
import com.ssafy.nyamnyam.global.security.jwt.JwtAccessDeniedHandler;
import com.ssafy.nyamnyam.global.security.jwt.JwtAuthenticationEntryPoint;
import com.ssafy.nyamnyam.global.security.jwt.JwtProvider;
import com.ssafy.nyamnyam.global.security.oauth2.CustomOAuth2UserService;
import com.ssafy.nyamnyam.global.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.ssafy.nyamnyam.global.security.oauth2.OAuth2AuthenticationSuccessHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeedController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class FeedControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean FeedService feedService;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean CustomOAuth2UserService oauthUsers;
    @MockitoBean OAuth2AuthenticationSuccessHandler oauthSuccess;
    @MockitoBean OAuth2AuthenticationFailureHandler oauthFailure;

    @Test
    @DisplayName("취미 정보 피드 목록을 인증 없이 공개 페이징 조회할 수 있다")
    void getFeedsWithoutAuth() throws Exception {
        FeedItemResponse mockFeed = new FeedItemResponse(
                501L,
                3L,
                "클라이밍",
                FeedType.EVENT,
                "2026 전국 청소년 스포츠클라이밍 페스티벌",
                "스포츠클라이밍 꿈나무들을 위한 축제",
                "한국관광공사 TourAPI",
                "https://korean.visitkorea.or.kr/detail/...",
                "https://cdn.example.com/events/climbing.jpg",
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 3),
                LocalDateTime.of(2026, 10, 18, 10, 0),
                false
        );

        PageResponse<FeedItemResponse> mockPage = new PageResponse<>(
                List.of(mockFeed),
                new PageResponse.PageInfo(0, 20, 1, 1, false)
        );

        given(feedService.getFeeds(3L, FeedType.EVENT, true, 0, 20))
                .willReturn(mockPage);

        mvc.perform(get("/api/v1/feeds")
                        .param("hobbyId", "3")
                        .param("type", "EVENT")
                        .param("excludeEnded", "true")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(501))
                .andExpect(jsonPath("$.data.content[0].hobbyName").value("클라이밍"))
                .andExpect(jsonPath("$.data.content[0].type").value("EVENT"))
                .andExpect(jsonPath("$.data.content[0].isEnded").value(false))
                .andExpect(jsonPath("$.data.page.totalElements").value(1));
    }
}

