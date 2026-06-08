package com.eodigaljido.backend.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("어디갈지도 Backend Swagger")
                        .description("""
                                ## 어디갈지도 백엔드 REST API

                                ### 인증
                                - 대부분의 API는 **JWT Bearer 토큰**이 필요합니다.
                                - `POST /auth/login` 또는 OAuth 로그인으로 `accessToken`을 발급받아 요청 헤더에 포함하세요.
                                - 헤더 형식: `Authorization: Bearer {accessToken}`
                                - 토큰 만료(401) 시 `POST /auth/token/refresh`로 재발급할 수 있습니다.

                                ### 에러 응답 형식
                                ```json
                                {
                                  "status": 400,
                                  "message": "에러 메시지",
                                  "timestamp": "2026-01-01T12:00:00"
                                }
                                ```

                                ### WebSocket 실시간 채팅
                                - 연결 엔드포인트: `ws://{host}/ws/chat` (SockJS 지원)
                                - CONNECT 헤더: `Authorization: Bearer {accessToken}`
                                - 메시지 수신 구독: `/topic/chat/{roomUuid}`
                                - 타이핑 인디케이터: `/topic/chat/{roomUuid}/typing`
                                """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("어디갈지도 팀")
                                .email("russeldestiny1234@gmail.com")))
                .servers(List.of(
                        new Server().url("/").description("현재 접속 주소"),
                        new Server().url("https://api.eodigaljido.uk").description("운영 서버"),
                        new Server().url("http://localhost:8080").description("로컬 개발 서버")
                ))
                .addSecurityItem(new SecurityRequirement().addList("Bearer"))
                .components(new Components()
                        .addSecuritySchemes("Bearer", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("로그인 후 발급받은 accessToken을 입력하세요.")))
                .tags(List.of(
                        new Tag().name("Auth").description("로그인 / 인증 / OAuth"),
                        new Tag().name("User").description("회원 / 프로필 / 즐겨찾기 코스"),
                        new Tag().name("Course").description("코스 / 공개 루트 / 내 루트 / 공동 루트 / preview"),
                        new Tag().name("CourseSchedule").description("코스 약속 일정 생성, 조회, 수정, 삭제"),
                        new Tag().name("Friend").description("친구 / 친구 코드 / 초대 preview"),
                        new Tag().name("Group").description("모임 / 게시판 / 모임 채팅방"),
                        new Tag().name("Chat").description("채팅방 / 메시지 / REST + WebSocket"),
                        new Tag().name("Notification").description("알림"),
                        new Tag().name("Following News").description("팔로잉 소식"),
                        new Tag().name("Onboarding").description("온보딩 설문"),
                        new Tag().name("Home").description("홈 화면"),
                        new Tag().name("Weather").description("날씨")
                ));
    }
}
