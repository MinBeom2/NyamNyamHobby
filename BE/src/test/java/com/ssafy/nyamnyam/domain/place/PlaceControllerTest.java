package com.ssafy.nyamnyam.domain.place;

import com.ssafy.nyamnyam.domain.place.collect.PlaceCollectService;
import com.ssafy.nyamnyam.domain.place.controller.PlaceController;
import com.ssafy.nyamnyam.domain.place.dto.response.PlaceResponse;
import com.ssafy.nyamnyam.domain.place.entity.PlaceCategory;
import com.ssafy.nyamnyam.domain.place.service.PlaceService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PlaceController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class PlaceControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean PlaceService placeService;
    @MockitoBean PlaceCollectService placeCollectService;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean CustomOAuth2UserService oauthUsers;
    @MockitoBean OAuth2AuthenticationSuccessHandler oauthSuccess;
    @MockitoBean OAuth2AuthenticationFailureHandler oauthFailure;

    @Test
    @DisplayName("활동 장소 목록을 인증 없이 공개 조회할 수 있다")
    void searchPlacesWithoutAuth() throws Exception {
        PlaceResponse mockPlace = new PlaceResponse(
                105L,
                "서울볼더스 클라이밍 컴퍼니",
                PlaceCategory.STUDIO,
                "서울특별시 마포구 월드컵북로 123",
                new BigDecimal("37.5562145"),
                new BigDecimal("126.9213451"),
                "평일 10:00~23:00 / 주말 10:00~20:00",
                "1일 이용권 20,000원",
                "https://place.map.kakao.com/123456",
                "https://search.kakaocdn.net/thumb.jpg",
                "KAKAO_LOCAL",
                false
        );

        given(placeService.searchPlaces(1L, 3L, PlaceCategory.STUDIO))
                .willReturn(List.of(mockPlace));

        mvc.perform(get("/api/v1/places")
                        .param("regionId", "1")
                        .param("hobbyId", "3")
                        .param("category", "STUDIO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(105))
                .andExpect(jsonPath("$.data[0].name").value("서울볼더스 클라이밍 컴퍼니"))
                .andExpect(jsonPath("$.data[0].category").value("STUDIO"))
                .andExpect(jsonPath("$.data[0].needsInfoCheck").value(false));
    }
}
