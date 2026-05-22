package com.eodigaljido.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("어디갈지도 API")
                        .description("""
                                ## 어디갈지도 백엔드 REST API

                                ### 인증
                                - 대부분의 API는 **JWT Bearer 토큰**이 필요합니다.
                                - `POST /auth/login` 또는 OAuth 로그인으로 `accessToken`을 발급받아 요청 헤더에 포함하세요.
                                - 헤더 형식: `Authorization: Bearer {accessToken}`
                                - 토큰 만료(401) 시 `POST /auth/token/refresh`로 재발급하세요.

                                ### 비로그인 허용 API (🔓)
                                | 경로 | 설명 |
                                |------|------|
                                | `GET /api/courses/public` | 공유 코스 목록 |
                                | `GET /api/courses/{courseId}` | 공개 코스 상세 |
                                | `GET /api/courses/public/{courseId}/preview` | 코스 공유 링크 preview |
                                | `GET /api/friends/code/{friendCode}/preview` | 친구 초대 링크 preview |
                                | `GET /api/weather` | 날씨 |
                                | `GET /api/home/courses` | 홈 인기 코스 |

                                ### 에러 응답 형식
                                ```json
                                {
                                  "status": 400,
                                  "message": "에러 메시지",
                                  "timestamp": "2026-01-01T12:00:00"
                                }
                                ```

                                ### 공유 링크 도메인
                                - 코스 공유: `https://share.eodigaljido.rjsgud.com/courses/public/{courseId}`
                                - 친구 초대: `https://share.eodigaljido.rjsgud.com/friends/add/{friendCode}`
                                """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("어디갈지도 팀")
                                .email("russeldestiny1234@gmail.com")))
                .servers(List.of(
                        new Server().url("http://3.36.85.213:8080").description("Production"),
                        new Server().url("http://localhost:8080").description("Local")
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
                        new Tag().name("User").description("회원 / 프로필"),
                        new Tag().name("Course").description("코스 (공유루트 / 내루트 / preview)"),
                        new Tag().name("Friend").description("친구 / 친구 코드 / 초대 preview"),
                        new Tag().name("Chat").description("채팅방 / 메시지"),
                        new Tag().name("Notification").description("알림"),
                        new Tag().name("Following News").description("팔로잉 소식"),
                        new Tag().name("Onboarding").description("온보딩 설문"),
                        new Tag().name("Home").description("홈 화면"),
                        new Tag().name("Weather").description("날씨")
                ))
                .externalDocs(new ExternalDocumentation()
                        .description("공유·친구 초대 기능 백엔드 작업 명세")
                        .url("https://share.eodigaljido.rjsgud.com"));
    }
}
