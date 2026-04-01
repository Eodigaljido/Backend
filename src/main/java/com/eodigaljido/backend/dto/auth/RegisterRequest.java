package com.eodigaljido.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청")
public record RegisterRequest(
        @Schema(description = "이메일 주소 (중복 불가)", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Email String email,

        @Schema(description = "비밀번호 (8~100자)", example = "password123!", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(min = 8, max = 100) String password,

        @Schema(description = "닉네임 (2~50자, 중복 불가)", example = "어디갈지몰라", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(min = 2, max = 50) String nickname
) {}
