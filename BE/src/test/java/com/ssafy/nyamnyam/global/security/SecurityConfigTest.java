package com.ssafy.nyamnyam.global.security;

import com.ssafy.nyamnyam.domain.hobby.controller.HobbyVideoController;
import com.ssafy.nyamnyam.domain.hobby.service.HobbyVideoService;
import com.ssafy.nyamnyam.domain.post.controller.PostController;
import com.ssafy.nyamnyam.domain.post.service.PostService;
import com.ssafy.nyamnyam.global.security.jwt.JwtAccessDeniedHandler;
import com.ssafy.nyamnyam.global.security.jwt.JwtAuthenticationEntryPoint;
import com.ssafy.nyamnyam.global.security.jwt.JwtProvider;
import com.ssafy.nyamnyam.global.security.oauth2.CustomOAuth2UserService;
import com.ssafy.nyamnyam.global.security.oauth2.OAuth2AuthenticationFailureHandler;
import com.ssafy.nyamnyam.global.security.oauth2.OAuth2AuthenticationSuccessHandler;
import com.ssafy.nyamnyam.global.upload.ImageUploadService;
import com.ssafy.nyamnyam.global.upload.UploadController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({HobbyVideoController.class, PostController.class, UploadController.class})
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class SecurityConfigTest {
    @Autowired MockMvc mvc;
    @MockitoBean HobbyVideoService hobbyVideos;
    @MockitoBean PostService posts;
    @MockitoBean ImageUploadService images;
    @MockitoBean JwtProvider jwtProvider;
    @MockitoBean CustomOAuth2UserService oauthUsers;
    @MockitoBean OAuth2AuthenticationSuccessHandler oauthSuccess;
    @MockitoBean OAuth2AuthenticationFailureHandler oauthFailure;

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/hobbies/1/posts", "/api/v1/posts/1", "/api/v1/hobbies/1/shorts"})
    void communityAndRelatedShortReadsArePublic(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isOk());
    }

    @Test
    void missingImageReturnsNotFoundWithoutRequiringLogin() throws Exception {
        mvc.perform(get("/api/v1/uploads/images/post/missing.png")).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @CsvSource({
            "POST, /api/v1/posts",
            "PATCH, /api/v1/posts/1",
            "DELETE, /api/v1/posts/1",
            "PUT, /api/v1/posts/1/like",
            "DELETE, /api/v1/posts/1/like",
            "POST, /api/v1/uploads/images",
            "PUT, /api/v1/hobbies/1/dislike",
            "DELETE, /api/v1/hobbies/1/dislike",
            "POST, /api/v1/hobbies/1/shorts",
            "POST, /api/v1/explorations",
            "GET, /api/v1/explorations/1/next",
            "GET, /api/v1/recommendations/1"
    })
    void writesAndPersonalResourcesStillRequireLogin(String method, String path) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), path)).andExpect(status().isUnauthorized());
        verifyNoInteractions(hobbyVideos, posts, images);
    }
}
