package com.eodigaljido.backend.config;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.eodigaljido.backend.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final Environment environment;

    @Value("${cors.allowed-origins:https://api.eodigaljido.uk,https://eodigaljido.uk,https://share.eodigaljido.rjsgud.com,https://share-staging.eodigaljido.rjsgud.com,http://localhost:5173,http://localhost:3000,http://localhost:8080}")
    private String[] allowedOrigins;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/test.html",
            "/auth/login", "/auth/register",
            "/auth/token/refresh",
            "/auth/oauth/google", "/auth/oauth/kakao", "/auth/oauth/config",
            "/auth/phone/code", "/auth/phone/verify",
            "/auth/find-account/send", "/auth/find-account/verify",
            "/auth/reset-password/send", "/auth/reset-password/verify",
            "/ws/chat/**",
            "/images/**",
            "/api/weather",
            "/api/home/**",
            "/api/courses/public"
    };

    // Swagger 엔드포인트 (전체 환경 공개 허용)
    private static final String[] SWAGGER_ENDPOINTS = {
            "/swagger", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**"
    };

    private static final String[] ACTUATOR_ENDPOINTS = {
            "/actuator/health", "/actuator/info"
    };

    private static final String COURSE_UUID_PATH_PATTERN =
            "/api/courses/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";

    static final RegexRequestMatcher PUBLIC_COURSE_DETAIL_MATCHER =
            RegexRequestMatcher.regexMatcher(HttpMethod.GET, "^" + COURSE_UUID_PATH_PATTERN + "$");

    static final RegexRequestMatcher PUBLIC_COURSE_REVIEW_MATCHER =
            RegexRequestMatcher.regexMatcher(HttpMethod.POST, "^" + COURSE_UUID_PATH_PATTERN + "/reviews$");

    private static final String[] WEBSOCKET_ALLOWED_ORIGINS= {
            "http://localhost:3000", "http://localhost:5173", "exp://172.28.22.99:8081", "http://localhost:8081", "*"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        boolean isLocalProfile = Arrays.asList(environment.getActiveProfiles()).contains("local");

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 보안 헤더 설정
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentTypeOptions(cto -> {})  // X-Content-Type-Options: nosniff (기본값)
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .preload(true)
                                .maxAgeInSeconds(31536000))
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .permissionsPolicyHeader(permissions -> permissions
                                .policy("camera=(), microphone=(), geolocation=(self), payment=()"))
                )
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll();
                    auth.requestMatchers(PUBLIC_ENDPOINTS).permitAll();
                    auth.requestMatchers(ACTUATOR_ENDPOINTS).permitAll();
                    auth.requestMatchers(SWAGGER_ENDPOINTS).permitAll();
                    // 코스 상세 조회 및 리뷰 작성은 GET/POST 모두 공개 (인증 선택)
                    auth.requestMatchers(PUBLIC_COURSE_DETAIL_MATCHER).permitAll();
                    auth.requestMatchers(PUBLIC_COURSE_REVIEW_MATCHER).permitAll();
                    // 공유 링크 preview — 비로그인 허용 (share-web, 카톡 인앱 브라우저)
                    auth.requestMatchers(HttpMethod.GET, "/api/courses/public/*/preview").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/friends/code/*/preview").permitAll();
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write(
                                    "{\"status\":401,\"message\":\"인증이 필요합니다.\",\"timestamp\":\"" +
                                    LocalDateTime.now() + "\"}"
                            );
                        })
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
