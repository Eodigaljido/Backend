package com.eodigaljido.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "이메일/비밀번호 로그인 요청")
public record LoginRequest(
        @Schema(description = "가입한 이메일 주소", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Email String email,

        @Schema(description = "비밀번호 (8자 이상)", example = "password123!", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String password
) {}
