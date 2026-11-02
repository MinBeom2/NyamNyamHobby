package com.ssafy.nyamnyam.domain.place;

import com.ssafy.nyamnyam.domain.place.controller.RegionController;
import com.ssafy.nyamnyam.domain.place.dto.response.SidoResponse;
import com.ssafy.nyamnyam.domain.place.service.RegionService;
import com.ssafy.nyamnyam.domain.user.dto.response.RegionResponse;
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

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegionController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class RegionControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean RegionService regionService;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean CustomOAuth2UserService oauthUsers;
    @MockitoBean OAuth2AuthenticationSuccessHandler oauthSuccess;
    @MockitoBean OAuth2AuthenticationFailureHandler oauthFailure;

    @Test
    @DisplayName("sido 파라미터가 없으면 시·도 목록을 공개 조회할 수 있다 (인증 불필요)")
    void getSidoListWithoutAuth() throws Exception {
        given(regionService.getSidoList()).willReturn(List.of(
                new SidoResponse("서울특별시", true),
                new SidoResponse("경기도", false)
        ));

        mvc.perform(get("/api/v1/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].sido").value("서울특별시"))
                .andExpect(jsonPath("$.data[0].enabled").value(true))
                .andExpect(jsonPath("$.data[1].sido").value("경기도"))
                .andExpect(jsonPath("$.data[1].enabled").value(false));
    }

    @Test
    @DisplayName("sido 파라미터가 전달되면 해당 시·도의 시·군·구 목록을 반환한다")
    void getSigunguList() throws Exception {
        given(regionService.getSigunguList("서울특별시")).willReturn(List.of(
                new RegionResponse(1L, "1111000000", "서울특별시", "종로구"),
                new RegionResponse(2L, "1114000000", "서울특별시", "중구")
        ));

        mvc.perform(get("/api/v1/regions").param("sido", "서울특별시"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].sigungu").value("종로구"))
                .andExpect(jsonPath("$.data[1].sigungu").value("중구"));
    }
}

