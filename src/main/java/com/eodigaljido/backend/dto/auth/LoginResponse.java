package com.eodigaljido.backend.dto.auth;

import com.eodigaljido.backend.domain.user.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 / 회원가입 응답")
public record LoginResponse(
        @Schema(description = "API 인증용 JWT (Authorization: Bearer 헤더에 사용)", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,

        @Schema(description = "토큰 재발급용 JWT (유효기간 30일)", example = "eyJhbGciOiJIUzI1NiJ9...")
        String refreshToken,

        @Schema(description = "토큰 타입", example = "Bearer")
        String tokenType,

        @Schema(description = "access token 만료까지 남은 시간(초)", example = "3600")
        long expiresIn,

        @Schema(description = "로그인한 사용자 기본 정보")
        UserInfo user
) {
    @Schema(description = "사용자 기본 정보")
    public record UserInfo(
            @Schema(description = "사용자 고유 ID", example = "1")
            Long id,

            @Schema(description = "사용자 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
            String uuid,

            @Schema(description = "이메일 주소 (OAuth 가입자는 null일 수 있음)", example = "user@example.com")
            String email,

            @Schema(description = "닉네임", example = "어디갈지몰라")
            String nickname,

            @Schema(description = "권한 (USER / ADMIN)", example = "USER")
            String role
    ) {}

    public static LoginResponse of(String accessToken, String refreshToken,
                                   long expiresIn, User user, String nickname) {
        return new LoginResponse(
                accessToken, refreshToken, "Bearer", expiresIn,
                new UserInfo(user.getId(), user.getUuid(), user.getEmail(), nickname, user.getRole().name())
        );
    }
}
