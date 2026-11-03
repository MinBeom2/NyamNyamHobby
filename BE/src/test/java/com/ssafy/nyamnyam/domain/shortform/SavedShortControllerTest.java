package com.ssafy.nyamnyam.domain.shortform;

import com.ssafy.nyamnyam.domain.shortform.controller.SavedShortController;
import com.ssafy.nyamnyam.domain.shortform.dto.response.SavedShortResponse;
import com.ssafy.nyamnyam.domain.shortform.dto.response.SavedShortSummaryResponse.HobbyCount;
import com.ssafy.nyamnyam.domain.shortform.entity.ShortStatus;
import com.ssafy.nyamnyam.domain.shortform.repository.ShortRepository;
import com.ssafy.nyamnyam.domain.shortform.repository.ShortSaveRepository;
import com.ssafy.nyamnyam.domain.shortform.service.ShortSaveService;
import com.ssafy.nyamnyam.global.security.SecurityConfig;
import com.ssafy.nyamnyam.global.security.UserPrincipal;
import com.ssafy.nyamnyam.global.security.jwt.*;
import com.ssafy.nyamnyam.global.security.oauth2.*;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SavedShortController.class)
@Import({ShortSaveService.class, SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class SavedShortControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ShortSaveRepository saves;
    @MockitoBean ShortRepository shorts;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean CustomOAuth2UserService oauthUsers;
    @MockitoBean OAuth2AuthenticationSuccessHandler oauthSuccess;
    @MockitoBean OAuth2AuthenticationFailureHandler oauthFailure;

    private UserPrincipal principal() { return new UserPrincipal(1L, "saved@test.local", "USER"); }

    @Test void listsOnlyAuthenticatedUsersSavesWithUtcAndNullableHobby() throws Exception {
        given(saves.count(1L, null)).willReturn(1L);
        given(saves.findSaved(1L, null, 0, 20, false)).willReturn(List.of(new SavedShortResponse(
                41L, "abcdefghijk", "보관 영상", null, null, ShortStatus.UNAVAILABLE,
                OffsetDateTime.parse("2026-10-20T05:20:00Z"))));
        mvc.perform(get("/api/v1/users/me/saved-shorts").param("userId", "2").with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].shortId").value(41))
                .andExpect(jsonPath("$.data.content[0].saveId").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].hobby").isEmpty())
                .andExpect(jsonPath("$.data.content[0].status").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.data.content[0].savedAt").value("2026-10-20T05:20:00Z"))
                .andExpect(jsonPath("$.data.page.totalElements").value(1))
                .andExpect(jsonPath("$.data.page.hasNext").value(false));
        verify(saves).findSaved(1L, null, 0, 20, false);
    }

    @Test void forwardsFilterPaginationAndOldestSort() throws Exception {
        given(saves.count(1L, 3L)).willReturn(6L);
        given(saves.findSaved(1L, 3L, 1, 5, true)).willReturn(List.of());
        mvc.perform(get("/api/v1/users/me/saved-shorts?hobbyId=3&page=1&size=5&sort=oldest").with(user(principal())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.page.totalPages").value(2))
                .andExpect(jsonPath("$.data.page.number").value(1));
        verify(saves).findSaved(1L, 3L, 1, 5, true);
    }

    @Test void summaryCountsAllHobbiesIncludingUnclassified() throws Exception {
        given(saves.countByHobby(1L)).willReturn(List.of(new HobbyCount(3L, "클라이밍", 6), new HobbyCount(null, null, 2)));
        mvc.perform(get("/api/v1/users/me/saved-shorts/summary").with(user(principal())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalCount").value(8))
                .andExpect(jsonPath("$.data.hobbies[0].count").value(6))
                .andExpect(jsonPath("$.data.hobbies[1].id").isEmpty());
    }

    @ParameterizedTest @ValueSource(strings = {"", "/summary"})
    void requiresAuthentication(String suffix) throws Exception {
        mvc.perform(get("/api/v1/users/me/saved-shorts" + suffix)).andExpect(status().isUnauthorized());
        verifyNoInteractions(saves);
    }

    @ParameterizedTest @ValueSource(strings = {"page=-1", "size=0", "size=101", "hobbyId=0", "hobbyId=-1",
            "sort=invalid", "sort=recent%27", "page=abc", "hobbyId=abc", "size=2147483648"})
    void invalidParametersReturn400(String query) throws Exception {
        mvc.perform(get("/api/v1/users/me/saved-shorts?" + query).with(user(principal())))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("COMMON_INVALID_INPUT"));
        verifyNoInteractions(saves);
    }
}
