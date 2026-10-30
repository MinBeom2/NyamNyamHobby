package com.ssafy.nyamnyam.global.security;

import com.ssafy.nyamnyam.global.security.jwt.JwtAccessDeniedHandler;
import com.ssafy.nyamnyam.global.security.jwt.JwtAuthenticationEntryPoint;
import com.ssafy.nyamnyam.global.security.jwt.JwtAuthenticationFilter;
import com.ssafy.nyamnyam.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final com.ssafy.nyamnyam.global.security.oauth2.CustomOAuth2UserService customOAuth2UserService;
    private final com.ssafy.nyamnyam.global.security.oauth2.OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final com.ssafy.nyamnyam.global.security.oauth2.OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // 인증 및 OAuth2 관련 엔드포인트 공개
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        // Actuator health
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // 커뮤니티 게시판·업로드 이미지·취미 검색·상세·플랜 미리보기·취미별 관련 숏폼 조회는 공개. 변경 요청은 인증 필요.
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/hobbies",
                                "/api/v1/hobbies/*",
                                "/api/v1/hobbies/*/plan-preview",
                                "/api/v1/hobbies/*/posts",
                                "/api/v1/posts/*",
                                "/api/v1/posts/*/comments",
                                "/api/v1/uploads/images/**",
                                "/api/v1/hobbies/*/shorts").permitAll()
                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler))
                .addFilterBefore(new JwtAuthenticationFilter(jwtProvider),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendUrl));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true); // refresh_token 쿠키 전송을 위해
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
