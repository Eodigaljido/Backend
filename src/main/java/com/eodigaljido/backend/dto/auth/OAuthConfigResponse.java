package com.eodigaljido.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OAuth 공개 설정 (클라이언트 ID)")
public record OAuthConfigResponse(
        @Schema(description = "Google OAuth 클라이언트 ID")
        String googleClientId,

        @Schema(description = "Kakao OAuth 클라이언트 ID (REST API 키)")
        String kakaoClientId
) {}
