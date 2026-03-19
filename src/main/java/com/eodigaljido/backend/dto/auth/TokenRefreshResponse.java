package com.eodigaljido.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "토큰 재발급 응답")
public record TokenRefreshResponse(
        @Schema(description = "새로 발급된 access token JWT", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,

        @Schema(description = "토큰 타입", example = "Bearer")
        String tokenType,

        @Schema(description = "access token 만료까지 남은 시간(초)", example = "3600")
        long expiresIn
) {
    public static TokenRefreshResponse of(String accessToken, long expiresIn) {
        return new TokenRefreshResponse(accessToken, "Bearer", expiresIn);
    }
}
