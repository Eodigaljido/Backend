package com.eodigaljido.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "소셜 로그인 요청")
public record OAuthLoginRequest(
        @Schema(
                description = "OAuth 인가 코드. 프론트엔드가 Google/Kakao 로그인 페이지에서 리다이렉트로 전달받은 code 파라미터 값",
                example = "4/0AY0e-g6...",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank String code,

        @Schema(
                description = "인가 코드 발급 시 사용한 redirect_uri. 생략하면 서버 설정값 사용 (프로덕션), 테스트 페이지 등 별도 URI 사용 시 명시 필요",
                example = "http://localhost:8080/test.html"
        )
        String redirectUri
) {}
