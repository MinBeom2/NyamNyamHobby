package com.ssafy.nyamnyam.domain.hobby.controller;

import com.ssafy.nyamnyam.domain.hobby.dto.request.HobbySearchCondition;
import com.ssafy.nyamnyam.domain.hobby.dto.response.HobbyDetailResponse;
import com.ssafy.nyamnyam.domain.hobby.dto.response.HobbySummaryResponse;
import com.ssafy.nyamnyam.domain.hobby.dto.response.PlanPreviewResponse;
import com.ssafy.nyamnyam.domain.hobby.service.HobbyService;
import com.ssafy.nyamnyam.domain.hobby.service.PlanPreviewService;
import com.ssafy.nyamnyam.global.common.PageResponse;
import com.ssafy.nyamnyam.global.error.CustomException;
import com.ssafy.nyamnyam.global.error.ErrorCode;
import com.ssafy.nyamnyam.global.security.SecurityConfig;
import com.ssafy.nyamnyam.global.security.jwt.JwtAccessDeniedHandler;
import com.ssafy.nyamnyam.global.security.jwt.JwtAuthenticationEntryPoint;
import com.ssafy.nyamnyam.global.security.jwt.JwtProvider;
import com.ssafy.nyamnyam.global.security.oauth2.CustomOAuth2UserService;
import com.ssafy.nyamnyam.global.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.ssafy.nyamnyam.global.security.oauth2.OAuth2AuthenticationSuccessHandler;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 취미 상세는 공개 경로이고, 로그인 토큰이 있으면 그 사용자 기준 myStatus를 내려 준다 */
@WebMvcTest(HobbyController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class HobbyControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean HobbyService hobbyService;
    @MockitoBean PlanPreviewService planPreviewService;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean CustomOAuth2UserService oauthUsers;
    @MockitoBean OAuth2AuthenticationSuccessHandler oauthSuccess;
    @MockitoBean OAuth2AuthenticationFailureHandler oauthFailure;

    private static HobbyDetailResponse sample(String myStatus) {
        return new HobbyDetailResponse(3L, "클라이밍",
                new HobbyDetailResponse.CategoryResponse(1L, "운동·야외"),
                "요약", "소개", null, "INDOOR", "BOTH", "NORMAL",
                new HobbyDetailResponse.RangeResponse(60, 120),
                new HobbyDetailResponse.RangeResponse(30000, 80000),
                new HobbyDetailResponse.RangeResponse(100000, 150000),
                "암벽화", "실내 암벽장", "1일 체험", "스트레칭 필수",
                List.of("실내", "활동적"), 420, myStatus);
    }

    @Test
    @DisplayName("취미 검색은 공개이고, 쿼리 문자열이 조건으로 바뀌어 서비스에 전달된다")
    void searchIsPublicAndParsesQuery() throws Exception {
        HobbySummaryResponse card = new HobbySummaryResponse(3L, "클라이밍",
                new HobbyDetailResponse.CategoryResponse(1L, "운동·야외"), "요약", null,
                "NORMAL", "INDOOR", "BOTH",
                new HobbyDetailResponse.RangeResponse(60, 120),
                new HobbyDetailResponse.RangeResponse(100000, 150000), 420, List.of("실내"));
        given(hobbyService.search(argThat(c -> "클라이".equals(c.keyword())
                        && c.time() == HobbySearchCondition.TimeRange.H1_TO_2H
                        && c.place() == HobbySearchCondition.PlaceFilter.INDOOR
                        && c.sort() == HobbySearchCondition.SortType.NAME
                        && c.page() == 1 && c.size() == 5), isNull()))
                .willReturn(new PageResponse<>(List.of(card), new PageResponse.PageInfo(1, 5, 6, 2, false)));

        mvc.perform(get("/api/v1/hobbies")
                        .param("keyword", "클라이").param("time", "1H_TO_2H").param("place", "INDOOR")
                        .param("sort", "NAME").param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(3))
                .andExpect(jsonPath("$.data.content[0].tags[0]").value("실내"))
                .andExpect(jsonPath("$.data.page.number").value(1))
                .andExpect(jsonPath("$.data.page.totalElements").value(6))
                .andExpect(jsonPath("$.data.page.hasNext").value(false));
    }

    @Test
    @DisplayName("잘못된 검색 조건은 서비스를 부르지 않고 400 COMMON_INVALID_INPUT")
    void searchRejectsInvalidQuery() throws Exception {
        mvc.perform(get("/api/v1/hobbies").param("keyword", "클"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_INPUT"));
        mvc.perform(get("/api/v1/hobbies").param("cost", "CHEAP"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(hobbyService);
    }

    @Test
    @DisplayName("플랜 미리보기는 공개이고 skillLevel·weeks가 서비스에 전달된다")
    void planPreviewIsPublic() throws Exception {
        PlanPreviewResponse.Item item = new PlanPreviewResponse.Item(1, "체험 수업 안내 확인하기", "설명", 15, "운동복", "기준");
        given(planPreviewService.preview(3L, "BEGINNER", 3)).willReturn(new PlanPreviewResponse(
                true, "BEGINNER", "클라이밍 · 입문 3주 계획", 3, 3,
                List.of(new PlanPreviewResponse.Week(1, "환경과 안전 수칙 익히기", List.of(item)))));

        mvc.perform(get("/api/v1/hobbies/3/plan-preview").param("skillLevel", "BEGINNER").param("weeks", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ready").value(true))
                .andExpect(jsonPath("$.data.totalWeeks").value(3))
                .andExpect(jsonPath("$.data.weeks[0].title").value("환경과 안전 수칙 익히기"))
                .andExpect(jsonPath("$.data.weeks[0].items[0].expectedMinutes").value(15));
    }

    @Test
    @DisplayName("플랜 미리보기: 파라미터를 생략하면 null로 넘겨 서비스 기본값을 쓴다")
    void planPreviewDefaults() throws Exception {
        given(planPreviewService.preview(3L, null, null)).willReturn(PlanPreviewResponse.notReady("BEGINNER"));

        mvc.perform(get("/api/v1/hobbies/3/plan-preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ready").value(false))
                .andExpect(jsonPath("$.data.weeks").isEmpty());
    }

    @Test
    @DisplayName("비회원도 취미 상세를 볼 수 있고 응답 봉투·필드가 명세와 같다")
    void detailIsPublic() throws Exception {
        given(hobbyService.getDetail(eq(3L), isNull())).willReturn(sample("NONE"));

        mvc.perform(get("/api/v1/hobbies/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.error").isEmpty())
                .andExpect(jsonPath("$.data.id").value(3))
                .andExpect(jsonPath("$.data.category.name").value("운동·야외"))
                .andExpect(jsonPath("$.data.sessionMinutes.min").value(60))
                .andExpect(jsonPath("$.data.monthlyCost.max").value(150000))
                .andExpect(jsonPath("$.data.tags[0]").value("실내"))
                .andExpect(jsonPath("$.data.thumbnailUrl").isEmpty())
                .andExpect(jsonPath("$.data.myStatus").value("NONE"));
    }

    @Test
    @DisplayName("로그인 토큰이 있으면 그 사용자 ID로 조회해 myStatus를 내려 준다")
    void detailWithLoginToken() throws Exception {
        given(jwtProvider.validateToken("token")).willReturn(true);
        given(jwtProvider.parseClaims("token")).willReturn(
                Jwts.claims().subject("7").add("email", "user@test.com").add("role", "USER").build());
        given(hobbyService.getDetail(3L, 7L)).willReturn(sample("SAVED"));

        mvc.perform(get("/api/v1/hobbies/3").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myStatus").value("SAVED"));
    }

    @Test
    @DisplayName("없는 취미는 404 COMMON_NOT_FOUND 봉투로 내려간다")
    void detailNotFound() throws Exception {
        given(hobbyService.getDetail(eq(99L), isNull())).willThrow(new CustomException(ErrorCode.COMMON_NOT_FOUND));

        mvc.perform(get("/api/v1/hobbies/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON_NOT_FOUND"));
    }
}
