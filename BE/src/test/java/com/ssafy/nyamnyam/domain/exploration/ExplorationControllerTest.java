package com.ssafy.nyamnyam.domain.exploration;

import com.ssafy.nyamnyam.domain.exploration.controller.ExplorationController;
import com.ssafy.nyamnyam.domain.exploration.dto.response.ExplorationProgressResponse;
import com.ssafy.nyamnyam.domain.exploration.dto.response.MergeExplorationResponse;
import com.ssafy.nyamnyam.domain.exploration.entity.ReactionType;
import com.ssafy.nyamnyam.domain.exploration.service.ExplorationService;
import com.ssafy.nyamnyam.domain.recommendation.dto.response.FinishExplorationResult;
import com.ssafy.nyamnyam.domain.recommendation.dto.response.RecommendationResponse;
import com.ssafy.nyamnyam.domain.recommendation.service.RecommendationService;
import com.ssafy.nyamnyam.domain.shortform.controller.ShortController;
import com.ssafy.nyamnyam.domain.shortform.dto.response.ShortSaveResponse;
import com.ssafy.nyamnyam.domain.shortform.service.ShortSaveService;
import com.ssafy.nyamnyam.domain.user.entity.User;
import com.ssafy.nyamnyam.domain.user.repository.UserRepository;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import com.ssafy.nyamnyam.global.security.SecurityConfig;
import com.ssafy.nyamnyam.global.security.UserPrincipal;
import com.ssafy.nyamnyam.global.security.jwt.*;
import com.ssafy.nyamnyam.global.security.oauth2.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({ExplorationController.class, ShortController.class})
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class ExplorationControllerTest {
    @MockitoBean ShortSaveService saves;
    @MockitoBean com.ssafy.nyamnyam.domain.shortform.service.ShortFeedService feeds;
    @Autowired MockMvc mvc;
    @MockitoBean ExplorationService explorations;
    @MockitoBean RecommendationService recommendations;
    @MockitoBean UserRepository users;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean CustomOAuth2UserService oauthUsers;
    @MockitoBean OAuth2AuthenticationSuccessHandler oauthSuccess;
    @MockitoBean OAuth2AuthenticationFailureHandler oauthFailure;

    private final UserPrincipal principal = new UserPrincipal(1L, "mvc@example.com", "USER");
    private User member;

    @Test void freeBrowseFeedIsPublicAndPassesLoginUserWhenPresent() throws Exception {
        given(feeds.feed(null, 7L, 10, 10)).willReturn(new com.ssafy.nyamnyam.domain.shortform.dto.response.ShortFeedResponse(java.util.List.of(), 7L, null, 10));
        mvc.perform(get("/api/v1/shorts/feed").param("seed", "7").param("cursor", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.seed").value(7))
                .andExpect(jsonPath("$.data.nextCursor").doesNotExist());
        given(feeds.feed(1L, null, 0, 5)).willReturn(new com.ssafy.nyamnyam.domain.shortform.dto.response.ShortFeedResponse(java.util.List.of(), 3L, 5, 10));
        mvc.perform(get("/api/v1/shorts/feed").param("size", "5").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nextCursor").value(5));
        verifyNoInteractions(explorations);
    }

    @Test void savesShortWithoutReactionOrSession() throws Exception {
        given(saves.save(1L, 100L)).willReturn(new ShortSaveResponse(100L, true));
        mvc.perform(put("/api/v1/shorts/100/save").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shortId").value(100))
                .andExpect(jsonPath("$.data.isSaved").value(true));
        verifyNoInteractions(explorations);
    }

    @Test void unsavesShortWithoutReactionOrSession() throws Exception {
        given(saves.unsave(1L, 100L)).willReturn(new ShortSaveResponse(100L, false));
        mvc.perform(delete("/api/v1/shorts/100/save").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isSaved").value(false));
        verifyNoInteractions(explorations);
    }

    @Test void savingRequiresAuthentication() throws Exception {
        mvc.perform(put("/api/v1/shorts/100/save")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/shorts/100/save")).andExpect(status().isUnauthorized());
        verifyNoInteractions(saves);
    }

    @BeforeEach void setUp() {
        member = User.builder().email("mvc@example.com").nickname("mvc").build();
        ReflectionTestUtils.setField(member, "id", 1L);
        given(users.findById(1L)).willReturn(Optional.of(member));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void finishUsesCreatedOrOkAndPreservesResponseBody(boolean created) throws Exception {
        RecommendationResponse response = RecommendationResponse.builder().recommendationId(88L)
                .items(List.of()).build();
        given(recommendations.finishExploration(50L, member)).willReturn(new FinishExplorationResult(response, created));

        mvc.perform(post("/api/v1/explorations/50/finish").with(user(principal)))
                .andExpect(status().is(created ? 201 : 200))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.recommendationId").value(88))
                .andExpect(jsonPath("$.data.created").doesNotExist())
                .andExpect(jsonPath("$.data.response").doesNotExist());
    }

    @Test void requiresAuthentication() throws Exception {
        mvc.perform(post("/api/v1/explorations/50/finish")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/explorations/50/views").contentType(MediaType.APPLICATION_JSON)
                .content("{\"shortId\":100}")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/shorts/100/reaction").contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"LIKE\",\"sessionId\":50}")).andExpect(status().isUnauthorized());
        verifyNoInteractions(explorations, recommendations);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"type\":\"LIKE\"}", "{\"type\":\"LIKE\",\"sessionId\":null}",
            "{\"type\":\"LIKE\",\"sessionId\":0}", "{\"type\":\"LIKE\",\"sessionId\":-1}"})
    void rejectsReactionWithoutPositiveSessionId(String body) throws Exception {
        mvc.perform(put("/api/v1/shorts/100/reaction").with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_INPUT"));
        verifyNoInteractions(explorations);
    }

    @Test void mapsSessionAndInactiveShortConflicts() throws Exception {
        doThrow(new CustomException(ErrorCode.SHORT_SESSION_ENDED)).when(explorations)
                .recordView(eq(50L), any(), eq(member));
        mvc.perform(post("/api/v1/explorations/50/views").with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"shortId\":100,\"watchedSeconds\":30}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("SHORT_SESSION_ENDED"));
        given(explorations.recordReaction(eq(100L), any(), eq(member)))
                .willThrow(new CustomException(ErrorCode.SHORT_NOT_ACTIVE));
        mvc.perform(put("/api/v1/shorts/100/reaction").with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"LIKE\",\"sessionId\":50}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("SHORT_NOT_ACTIVE"));
    }

    @Test void mapsMissingOrForeignSessionToNotFound() throws Exception {
        given(recommendations.finishExploration(50L, member)).willThrow(new CustomException(ErrorCode.COMMON_NOT_FOUND));
        mvc.perform(post("/api/v1/explorations/50/finish").with(user(principal)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("COMMON_NOT_FOUND"));
    }

    @Test void viewStillReturnsNoContent() throws Exception {
        mvc.perform(post("/api/v1/explorations/50/views").with(user(principal))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"shortId\":100,\"watchedSeconds\":30}"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(explorations).recordView(eq(50L), any(), eq(member));
    }

    @Test void progressPreservesContractFieldNamesAndIntegerPercentages() throws Exception {
        var progress = ExplorationProgressResponse.builder().currentStep(4).maxCount(12)
                .reactedHobbies(List.of(new ExplorationProgressResponse.ReactedHobbyResponse(3L, "클라이밍", ReactionType.LIKE)))
                .topInterests(List.of(new ExplorationProgressResponse.TopInterestResponse("클라이밍", 100),
                        new ExplorationProgressResponse.TopInterestResponse("러닝", 75))).build();
        given(explorations.getProgress(50L, member)).willReturn(progress);
        mvc.perform(get("/api/v1/explorations/50/progress").with(user(principal)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currentStep").value(4)).andExpect(jsonPath("$.data.maxCount").value(12))
                .andExpect(jsonPath("$.data.reactedHobbies[0].id").value(3))
                .andExpect(jsonPath("$.data.reactedHobbies[0].name").value("클라이밍"))
                .andExpect(jsonPath("$.data.reactedHobbies[0].reaction").value("LIKE"))
                .andExpect(jsonPath("$.data.topInterests[0].hobbyName").value("클라이밍"))
                .andExpect(jsonPath("$.data.topInterests[0].percent").value(100))
                .andExpect(jsonPath("$.data.topInterests[1].percent").value(75))
                .andExpect(jsonPath("$.data.topInterests[0].subjectName").doesNotExist());
    }

    @Test void progressRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/explorations/50/progress")).andExpect(status().isUnauthorized());
        verifyNoInteractions(explorations);
    }

    @Test
    void mergeGuestExploration_withGuestKeyCookie_returns200AndDeletesCookie() throws Exception {
        MergeExplorationResponse mergeResult = new MergeExplorationResponse(8, 1);
        given(explorations.mergeGuestData("test-guest-uuid", member)).willReturn(mergeResult);

        mvc.perform(post("/api/v1/explorations/merge")
                        .with(user(principal))
                        .cookie(new Cookie("guest_key", "test-guest-uuid")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mergedReactionsCount").value(8))
                .andExpect(jsonPath("$.data.mergedSessionCount").value(1))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("guest_key=")))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));

        verify(explorations).mergeGuestData("test-guest-uuid", member);
    }

    @Test
    void mergeGuestExploration_withoutGuestKeyCookie_returnsZeroCounts() throws Exception {
        MergeExplorationResponse mergeResult = new MergeExplorationResponse(0, 0);
        given(explorations.mergeGuestData(null, member)).willReturn(mergeResult);

        mvc.perform(post("/api/v1/explorations/merge")
                        .with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mergedReactionsCount").value(0))
                .andExpect(jsonPath("$.data.mergedSessionCount").value(0));
    }

    @Test
    void mergeGuestExploration_requiresAuthentication() throws Exception {
        mvc.perform(post("/api/v1/explorations/merge"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(explorations);
    }
}
