package com.ssafy.nyamnyam.domain.recommendation;

import com.ssafy.nyamnyam.domain.recommendation.controller.RecommendationController;
import com.ssafy.nyamnyam.domain.recommendation.model.HobbyMatch;
import com.ssafy.nyamnyam.domain.recommendation.model.TagFeature;
import com.ssafy.nyamnyam.domain.recommendation.service.HobbyRecommendationService;
import com.ssafy.nyamnyam.global.security.SecurityConfig;
import com.ssafy.nyamnyam.global.security.jwt.JwtProvider;
import com.ssafy.nyamnyam.global.security.jwt.JwtAuthenticationEntryPoint;
import com.ssafy.nyamnyam.global.security.jwt.JwtAccessDeniedHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RecommendationController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class RecommendationControllerTest {
    private static final String PATH = "/api/v1/recommendations/preview";
    @Autowired MockMvc mvc;
    @MockitoBean HobbyRecommendationService service;
    @MockitoBean JwtProvider jwtProvider;

    @Test void requiresAuthentication() throws Exception {
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{\"preferenceScores\":{}}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test void returnsRecommendationAndAppliesDefaults() throws Exception {
        when(service.recommendByPreference(Map.of(TagFeature.SPORTS, 3.0), 3, Set.of()))
                .thenReturn(List.of(new HobbyMatch(7, "수영", "요약", null, 0.8, 1)));
        mvc.perform(post(PATH).with(user("1")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferenceScores\":{\"SPORTS\":3.0}}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].hobbyId").value(7))
                .andExpect(jsonPath("$.data.items[0].rank").value(1))
                .andExpect(jsonPath("$.data.items[0].similarity").value(0.8));
        verify(service).recommendByPreference(Map.of(TagFeature.SPORTS, 3.0), 3, Set.of());
    }

    @Test void passesOptionsAndReturnsEmptyResult() throws Exception {
        when(service.recommendByPreference(Map.of(), 5, Set.of(2L))).thenReturn(List.of());
        mvc.perform(post(PATH).with(user("1")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferenceScores\":{},\"limit\":5,\"excludedHobbyIds\":[2]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items").isEmpty());
        verify(service).recommendByPreference(Map.of(), 5, Set.of(2L));
    }

    @Test void rejectsInvalidOrMalformedRequestsBeforeCallingService() throws Exception {
        for (String body : List.of("{}", "{", "{\"preferenceScores\":{\"UNKNOWN\":1}}",
                "{\"preferenceScores\":{\"SPORTS\":null}}", "{\"preferenceScores\":{},\"limit\":0}",
                "{\"preferenceScores\":{},\"excludedHobbyIds\":[-1]}",
                "{\"preferenceScores\":{},\"excludedHobbyIds\":[null]}")) {
            mvc.perform(post(PATH).with(user("1")).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_INPUT"));
        }
        verifyNoInteractions(service);
    }
}
