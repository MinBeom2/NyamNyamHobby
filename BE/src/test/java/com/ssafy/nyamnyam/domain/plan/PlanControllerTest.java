package com.ssafy.nyamnyam.domain.plan;

import com.ssafy.nyamnyam.domain.plan.controller.PlanController;
import com.ssafy.nyamnyam.domain.plan.dto.*;
import com.ssafy.nyamnyam.domain.plan.service.*;
import com.ssafy.nyamnyam.global.security.*;
import com.ssafy.nyamnyam.global.security.jwt.*;
import com.ssafy.nyamnyam.global.security.oauth2.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlanController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class PlanControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean PlanService service;
    @MockitoBean PlanQueryService query;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean CustomOAuth2UserService oauth;
    @MockitoBean OAuth2AuthenticationSuccessHandler success;
    @MockitoBean OAuth2AuthenticationFailureHandler failure;
    UserPrincipal principal() { return new UserPrincipal(1L, "todo@test.com", "USER"); }

    @Test void startReturnsCreatedWithRealPlanId() throws Exception {
        when(service.start(eq(1L), eq(3L), any())).thenReturn(new PlanResponses.Started(12L, 55L, "IN_PROGRESS", "계획"));
        mvc.perform(post("/api/v1/users/me/hobbies/3/start").with(user(principal()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"skillLevel\":\"BEGINNER\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.planId").value(55));
    }

    @Test void serializesRealPlanViewAndHomeCardsWithOffsetDatesAndCustomFlag() throws Exception {
        var entry = PlanTestFixture.entry(PlanTestFixture.user(1), PlanTestFixture.hobby(3), 12);
        var plan = PlanTestFixture.plan(entry);
        var renderer = new PlanQueryService(null, null, null, java.time.Clock.systemUTC());
        var view = renderer.view(plan, 1, false, PlanTestFixture.START);
        when(query.get(1L, 55L, null, false)).thenReturn(view);
        mvc.perform(get("/api/v1/plans/55").with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.startedAt").value("2026-10-02T00:00:00+09:00"))
                .andExpect(jsonPath("$.data.items[0].isCustom").value(false))
                .andExpect(jsonPath("$.data.planProgress.total").value(3))
                .andExpect(jsonPath("$.data.weekProgress.total").value(1));
        var card = com.ssafy.nyamnyam.domain.user.dto.response.UserHobbyResponse.from(entry);
        card.attachPlan(view);
        when(query.summary(1L)).thenReturn(new PlanResponses.Summary(1, 0, 1, 3,
                java.util.Map.of("IN_PROGRESS", 1L), java.util.List.of(card)));
        mvc.perform(get("/api/v1/users/me/todos/summary").with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plans[0].planId").value(55))
                .andExpect(jsonPath("$.data.plans[0].startedAt").value("2026-10-02T00:00:00+09:00"));
    }

    @Test void memoOnlyPatchPreservesOmissionAndExplicitNull() throws Exception {
        mvc.perform(patch("/api/v1/plans/55/items/901").with(user(principal()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"memo\":null}"))
                .andExpect(status().isOk());
        var capture = ArgumentCaptor.forClass(UpdatePlanItemRequest.class);
        verify(service).update(eq(1L), eq(55L), eq(901L), capture.capture());
        assertThat(capture.getValue().isMemoPresent()).isTrue();
        assertThat(capture.getValue().isStatusPresent()).isFalse();
        assertThat(capture.getValue().getMemo()).isNull();
    }

    @ParameterizedTest @ValueSource(strings = {"{}", "{\"status\":null}", "{\"status\":\"UNKNOWN\"}",
            "{\"weekNo\":null}", "{\"expectedMinutes\":null}", "{\"weekNo\":0}", "{\"weekNo\":32768}",
            "{\"expectedMinutes\":0}", "{\"expectedMinutes\":1441}",
            "{\"weekNo\":1.5}", "{\"expectedMinutes\":30.5}", "{\"weekNo\":\"2\"}",
            "{\"expectedMinutes\":true}", "{\"weekNo\":2147483648}"})
    void rejectsInvalidPatch(String body) throws Exception {
        mvc.perform(patch("/api/v1/plans/55/items/901").with(user(principal()))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("COMMON_INVALID_INPUT"));
        verifyNoInteractions(service);
    }

    @Test void rejectsMemoOver500CharactersAndAcceptsBoundary() throws Exception {
        mvc.perform(patch("/api/v1/plans/55/items/901").with(user(principal()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"memo\":\"" + "가".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/plans/55/items/901").with(user(principal()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"memo\":\"" + "가".repeat(500) + "\"}"))
                .andExpect(status().isOk());
        verify(service).update(eq(1L), eq(55L), eq(901L), any());
    }

    @ParameterizedTest @ValueSource(strings = {"{}", "{\"skillLevel\":null}", "{\"skillLevel\":\"EXPERT\"}"})
    void rejectsMissingOrInvalidSkill(String body) throws Exception {
        mvc.perform(post("/api/v1/users/me/hobbies/3/start").with(user(principal()))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @ParameterizedTest @ValueSource(strings = {"{}", "{\"status\":\"SAVED\"}", "{\"status\":\"COMPLETED\"}"})
    void onlyPermittedStatusTargetsAreAccepted(String body) throws Exception {
        mvc.perform(patch("/api/v1/users/me/hobbies/3/status").with(user(principal()))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void customItemValidationAndSuccessfulCreate() throws Exception {
        mvc.perform(post("/api/v1/plans/55/items").with(user(principal()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"weekNo\":1,\"title\":\" \",\"expectedMinutes\":10}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/plans/55/items").with(user(principal()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"weekNo\":2,\"title\":\"활동\",\"description\":\"설명\",\"expectedMinutes\":30}"))
                .andExpect(status().isCreated());
        verify(service).create(eq(1L), eq(55L), any());
    }

    @ParameterizedTest @ValueSource(strings = {
            "{\"weekNo\":1.5,\"title\":\"활동\",\"expectedMinutes\":30}",
            "{\"weekNo\":1,\"title\":\"활동\",\"expectedMinutes\":30.5}",
            "{\"weekNo\":1,\"title\":\"활동\",\"expectedMinutes\":\"30\"}"})
    void customItemRejectsNonIntegerJsonNumbers(String body) throws Exception {
        mvc.perform(post("/api/v1/plans/55/items").with(user(principal()))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test void readEndpointsBindWeekAndAuthenticate() throws Exception {
        mvc.perform(get("/api/v1/plans/55?week=3").with(user(principal()))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/plans/55/overview").with(user(principal()))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/users/me/todos/summary").with(user(principal()))).andExpect(status().isOk());
        verify(query).get(1L, 55L, 3, false);
        verify(query).get(1L, 55L, null, true);
        verify(query).summary(1L);
        mvc.perform(get("/api/v1/plans/55?week=abc").with(user(principal()))).andExpect(status().isBadRequest());
    }

    @Test void deletingItemReturns204AndStatusMutationUsesPrincipal() throws Exception {
        mvc.perform(delete("/api/v1/plans/55/items/901").with(user(principal()))).andExpect(status().isNoContent());
        mvc.perform(patch("/api/v1/users/me/hobbies/3/status").with(user(principal()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PAUSED\"}"))
                .andExpect(status().isOk());
        verify(service).delete(1L, 55L, 901L);
        verify(service).changeStatus(1L, 3L, new HobbyStatusRequest("PAUSED"));
    }

    @Test void everyPlanEndpointRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/plans/55")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/plans/55/overview")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/users/me/todos/summary")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/users/me/hobbies/3/start").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/v1/users/me/hobbies/3/status").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/v1/plans/55/items/901").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/plans/55/items").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/plans/55/items/901")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service, query);
    }
}
