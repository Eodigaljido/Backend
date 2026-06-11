package com.eodigaljido.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Google 로그인 요청 (Android 앱)")
public record GoogleOAuthLoginRequest(
        @Schema(
                description = "Android Google Sign-In SDK에서 발급받은 ID Token",
                example = "eyJhbGciOiJSUzI1NiIs...",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank String idToken
) {}
