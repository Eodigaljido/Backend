package com.eodigaljido.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "토큰 재발급 / 로그아웃 요청")
public record TokenRefreshRequest(
        @Schema(description = "로그인 시 발급받은 refresh token (JWT 문자열)", example = "eyJhbGciOiJIUzI1NiJ9...", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String refreshToken
) {}
