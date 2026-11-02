package com.ssafy.nyamnyam.domain.notification;

import com.ssafy.nyamnyam.domain.notification.controller.NotificationController;
import com.ssafy.nyamnyam.domain.notification.dto.NotificationResponse;
import com.ssafy.nyamnyam.domain.notification.service.NotificationService;
import com.ssafy.nyamnyam.global.common.PageResponse;
import com.ssafy.nyamnyam.global.security.SecurityConfig;
import com.ssafy.nyamnyam.global.security.jwt.JwtAccessDeniedHandler;
import com.ssafy.nyamnyam.global.security.jwt.JwtAuthenticationEntryPoint;
import com.ssafy.nyamnyam.global.security.jwt.JwtProvider;
import com.ssafy.nyamnyam.global.security.oauth2.CustomOAuth2UserService;
import com.ssafy.nyamnyam.global.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.ssafy.nyamnyam.global.security.oauth2.OAuth2AuthenticationSuccessHandler;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class NotificationControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean NotificationService notificationService;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean CustomOAuth2UserService oauthUsers;
    @MockitoBean OAuth2AuthenticationSuccessHandler oauthSuccess;
    @MockitoBean OAuth2AuthenticationFailureHandler oauthFailure;

    @BeforeEach
    void loginAs7() {
        given(jwtProvider.validateToken("token")).willReturn(true);
        given(jwtProvider.parseClaims("token")).willReturn(
                Jwts.claims().subject("7").add("email", "user@test.com").add("role", "USER").build());
    }

    @Test
    @DisplayName("로그인하지 않으면 모든 알림 API가 401")
    void requiresLogin() throws Exception {
        mvc.perform(get("/api/v1/notifications")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/notifications/unread-count")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/v1/notifications/1/read")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/v1/notifications/read-all")).andExpect(status().isUnauthorized());
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("목록: 본인 ID와 쿼리로 조회하고 페이지 봉투로 내려 준다")
    void list() throws Exception {
        NotificationResponse item = new NotificationResponse(1L, "COMMENT", "'암벽화 추천'에 댓글이 달렸어요", "산토끼: 추천해요",
                "/community/100", false, null, OffsetDateTime.of(2026, 11, 2, 9, 0, 0, 0, ZoneOffset.ofHours(9)));
        given(notificationService.list(7L, true, 0, 20))
                .willReturn(new PageResponse<>(List.of(item), new PageResponse.PageInfo(0, 20, 1, 1, false)));

        mvc.perform(get("/api/v1/notifications").header("Authorization", "Bearer token")
                        .param("unreadOnly", "true").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].type").value("COMMENT"))
                .andExpect(jsonPath("$.data.content[0].linkPath").value("/community/100"))
                .andExpect(jsonPath("$.data.content[0].isRead").value(false))
                .andExpect(jsonPath("$.data.page.totalElements").value(1));
    }

    @Test
    @DisplayName("안 읽은 수와 모두 읽음")
    void unreadCountAndReadAll() throws Exception {
        given(notificationService.unreadCount(7L)).willReturn(3L);
        given(notificationService.readAll(7L)).willReturn(3);

        mvc.perform(get("/api/v1/notifications/unread-count").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(3));
        mvc.perform(patch("/api/v1/notifications/read-all").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updatedCount").value(3));
    }
}
