package com.ssafy.nyamnyam.domain.user.controller;

import com.ssafy.nyamnyam.domain.user.dto.response.UserConditionsResponse;
import com.ssafy.nyamnyam.domain.user.dto.request.OnboardingRequest;
import com.ssafy.nyamnyam.domain.user.dto.response.OnboardingResponse;
import com.ssafy.nyamnyam.domain.user.dto.response.OnboardingOptionsResponse;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
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

    @Test void savesBothOnboardingScreensWithoutSkillLevel() throws Exception {
        given(userService.saveOnboarding(eq(1L), any())).willReturn(new OnboardingResponse(true, 2));
        mockMvc.perform(put("/api/v1/users/me/onboarding").with(user(createPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"initialHobbies":[{"hobbyId":3},{"hobbyId":4}],"weeklyHours":3,
                         "monthlyBudget":150000,"placePref":"INDOOR","socialPref":"ANY","regionId":1}
                        """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.onboarded").value(true))
                .andExpect(jsonPath("$.data.savedHobbyCount").value(2));
        var captor = ArgumentCaptor.forClass(OnboardingRequest.class);
        verify(userService).saveOnboarding(eq(1L), captor.capture());
        assertThat(captor.getValue().getInitialHobbies()).extracting(OnboardingRequest.InitialHobby::hobbyId)
                .containsExactly(3L, 4L);
        assertThat(captor.getValue().getWeeklyHours()).isEqualTo((short) 3);
        assertThat(captor.getValue().getMonthlyBudget()).isEqualTo(150000);
        assertThat(captor.getValue().getRegionId()).isEqualTo(1L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"initialHobbies\":[]}", "{\"initialHobbies\":null}",
            "{\"weeklyHours\":null,\"monthlyBudget\":null,\"placePref\":null,\"socialPref\":null,\"regionId\":null}"})
    void allOnboardingSelectionsAreOptional(String body) throws Exception {
        given(userService.saveOnboarding(eq(1L), any())).willReturn(new OnboardingResponse(true, 0));
        mockMvc.perform(put("/api/v1/users/me/onboarding").with(user(createPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.savedHobbyCount").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"initialHobbies\":[{\"hobbyId\":1},{\"hobbyId\":2},{\"hobbyId\":3},{\"hobbyId\":4},{\"hobbyId\":5},{\"hobbyId\":6}]}",
            "{\"initialHobbies\":[{}]}", "{\"initialHobbies\":[null]}",
            "{\"initialHobbies\":[{\"hobbyId\":0}]}", "{\"weeklyHours\":41}",
            "{\"weeklyHours\":-1}", "{\"monthlyBudget\":-1}", "{\"regionId\":0}",
            "{\"placePref\":\"BOTH\"}", "{\"socialPref\":\"\"}"})
    void rejectsInvalidOnboardingSelections(String body) throws Exception {
        mockMvc.perform(put("/api/v1/users/me/onboarding").with(user(createPrincipal()))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_INPUT"));
        verifyNoInteractions(userService);
    }

    @Test void providesRealOptionIds() throws Exception {
        given(userService.getOnboardingOptions()).willReturn(new OnboardingOptionsResponse(
                List.of(new OnboardingOptionsResponse.HobbyOption(3L, "클라이밍", 1L, "운동·야외")), List.of()));
        mockMvc.perform(get("/api/v1/users/me/onboarding/options").with(user(createPrincipal())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.hobbies[0].hobbyId").value(3));
    }

    @Test void onboardingRequiresAuthentication() throws Exception {
        mockMvc.perform(put("/api/v1/users/me/onboarding").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users/me/onboarding/options")).andExpect(status().isUnauthorized());
        verifyNoInteractions(userService);
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
