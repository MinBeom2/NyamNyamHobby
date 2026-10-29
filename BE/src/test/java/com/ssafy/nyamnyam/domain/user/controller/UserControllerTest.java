package com.ssafy.nyamnyam.domain.user.controller;

import com.ssafy.nyamnyam.domain.user.dto.response.UserConditionsResponse;
import com.ssafy.nyamnyam.domain.user.dto.response.UserHobbyResponse;
import com.ssafy.nyamnyam.domain.user.dto.response.UserProfileResponse;
import com.ssafy.nyamnyam.domain.user.service.UserService;
import com.ssafy.nyamnyam.global.security.SecurityConfig;
import com.ssafy.nyamnyam.global.security.UserPrincipal;
import com.ssafy.nyamnyam.global.security.jwt.JwtAccessDeniedHandler;
import com.ssafy.nyamnyam.global.security.jwt.JwtAuthenticationEntryPoint;
import com.ssafy.nyamnyam.global.security.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private com.ssafy.nyamnyam.global.security.oauth2.CustomOAuth2UserService customOAuth2UserService;

    @MockitoBean
    private com.ssafy.nyamnyam.global.security.oauth2.OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @MockitoBean
    private com.ssafy.nyamnyam.global.security.oauth2.OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    private UserPrincipal createPrincipal() {
        return new UserPrincipal(1L, "user@example.com", "USER");
    }

    @Test
    @DisplayName("GET /api/v1/users/me 내 프로필 조회")
    void getProfile_success() throws Exception {
        UserProfileResponse response = new UserProfileResponse(
                1L, "user@example.com", "클라이머호호", null, "USER", true, null
        );
        given(userService.getUserProfile(1L)).willReturn(response);

        mockMvc.perform(get("/api/v1/users/me")
                        .with(user(createPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("클라이머호호"));
    }

    @Test
    @DisplayName("PATCH /api/v1/users/me 프로필 수정")
    void updateProfile_success() throws Exception {
        UserProfileResponse response = new UserProfileResponse(
                1L, "user@example.com", "새닉네임", null, "USER", true, null
        );
        given(userService.updateProfile(eq(1L), any())).willReturn(response);

        mockMvc.perform(patch("/api/v1/users/me")
                        .with(user(createPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"새닉네임\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nickname").value("새닉네임"));
    }

    @Test
    @DisplayName("PUT /api/v1/users/me/conditions 이용 상황 수정")
    void updateConditions_success() throws Exception {
        UserConditionsResponse response = new UserConditionsResponse((short) 5, 200000, "INDOOR", "TOGETHER", null);
        given(userService.updateConditions(eq(1L), any())).willReturn(response);

        mockMvc.perform(put("/api/v1/users/me/conditions")
                        .with(user(createPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weeklyHours\":5,\"monthlyBudget\":200000,\"placePref\":\"INDOOR\",\"socialPref\":\"TOGETHER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.weeklyHours").value(5))
                .andExpect(jsonPath("$.data.monthlyBudget").value(200000));
    }

    @Test
    @DisplayName("PATCH /api/v1/users/me/password 비밀번호 변경")
    void changePassword_success() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .with(user(createPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"CurrentPwd1!\",\"newPassword\":\"NewPwd1234!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("DELETE /api/v1/users/me 회원 탈퇴")
    void withdraw_success() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me")
                        .with(user(createPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"Password123!\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/v1/users/me/hobbies 취미 목록 조회")
    void getUserHobbies_success() throws Exception {
        UserHobbyResponse hobby = new UserHobbyResponse(
                10L, 3L, "클라이밍", null, "IN_PROGRESS", "BEGINNER", null, 0.5, null
        );
        given(userService.getUserHobbies(eq(1L), any())).willReturn(List.of(hobby));

        mockMvc.perform(get("/api/v1/users/me/hobbies")
                        .with(user(createPrincipal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("클라이밍"))
                .andExpect(jsonPath("$.data[0].status").value("IN_PROGRESS"));
    }
}
