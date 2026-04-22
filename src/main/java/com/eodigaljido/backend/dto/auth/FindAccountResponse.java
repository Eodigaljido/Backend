package com.eodigaljido.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "아이디/이메일 찾기 결과")
public record FindAccountResponse(
        @Schema(description = "아이디 (설정하지 않은 경우 null)", example = "john123")
        String userId,

        @Schema(description = "이메일 (연동되지 않은 경우 null)", example = "user@example.com")
        String email,

        @Schema(description = "이메일 연동 여부", example = "true")
        boolean emailLinked
) {
    public static FindAccountResponse of(String userId, String email) {
        return new FindAccountResponse(userId, email, email != null);
    }
}
