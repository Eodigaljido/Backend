package com.eodigaljido.backend.dto.auth;

import com.eodigaljido.backend.domain.user.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "소셜 로그인 / 회원가입 응답")
public record OAuthLoginResponse(
        @Schema(description = "API 인증용 JWT (Authorization: Bearer 헤더에 사용)", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,

        @Schema(description = "토큰 재발급용 JWT (유효기간 30일)", example = "eyJhbGciOiJIUzI1NiJ9...")
        String refreshToken,

        @Schema(description = "토큰 타입", example = "Bearer")
        String tokenType,

        @Schema(description = "access token 만료까지 남은 시간(초)", example = "3600")
        long expiresIn,

        @Schema(description = "신규 가입 여부. true=이번에 처음 가입, false=기존 계정으로 로그인", example = "false")
        boolean isNewUser,

        @Schema(description = "로그인한 사용자 기본 정보")
        LoginResponse.UserInfo user
) {
    public static OAuthLoginResponse of(String accessToken, String refreshToken,
                                        long expiresIn, boolean isNewUser,
                                        User user, String nickname) {
        return new OAuthLoginResponse(
                accessToken, refreshToken, "Bearer", expiresIn, isNewUser,
                new LoginResponse.UserInfo(user.getId(), user.getUuid(), user.getUserId(), user.getEmail(), nickname, user.getRole().name())
        );
    }
}
