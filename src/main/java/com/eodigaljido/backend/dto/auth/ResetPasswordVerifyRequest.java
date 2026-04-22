package com.eodigaljido.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "비밀번호 재설정 — 인증 확인 및 새 비밀번호 설정")
public record ResetPasswordVerifyRequest(
        @Schema(description = "휴대폰 번호 (하이픈 없이)", example = "01012345678")
        @NotBlank String phone,

        @Schema(description = "6자리 인증번호", example = "123456")
        @NotBlank @Size(min = 6, max = 6) String code,

        @Schema(description = "새 비밀번호 (8~100자, 소문자·숫자·특수문자 각 1개 이상)", example = "newPass1!")
        @NotBlank
        @Size(min = 8, max = 100)
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&#^()\\-_=+\\[\\]{}|;:,.<>?/~`]).{8,}$",
                message = "비밀번호는 소문자, 숫자, 특수문자를 각각 최소 1개 이상 포함해야 합니다."
        )
        String newPassword
) {}
