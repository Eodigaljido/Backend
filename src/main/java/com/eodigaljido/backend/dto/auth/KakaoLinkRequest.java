package com.eodigaljido.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "카카오 계정 연동 요청")
public record KakaoLinkRequest(
        @Schema(
                description = "카카오 인가 코드",
                example = "FKqH4z...",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank String code,

        @Schema(
                description = "인가 코드 발급 시 사용한 redirect_uri. 생략하면 서버 설정값 사용",
                example = "http://localhost:8080/test.html"
        )
        String redirectUri
) {}
