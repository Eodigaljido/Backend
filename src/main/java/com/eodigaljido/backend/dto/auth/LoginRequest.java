package com.eodigaljido.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 요청 (아이디 또는 이메일 중 하나 필수)")
public record LoginRequest(
        @Schema(description = "사용자 아이디 (8자 이하) 또는 이메일 주소", example = "john123 또는 user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String identifier,

        @Schema(description = "비밀번호", example = "password123!", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String password
) {}
