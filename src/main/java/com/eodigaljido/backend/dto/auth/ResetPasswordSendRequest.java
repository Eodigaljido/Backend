package com.eodigaljido.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "비밀번호 재설정 — 인증번호 발송 요청")
public record ResetPasswordSendRequest(
        @Schema(description = "아이디 또는 이메일", example = "john123")
        @NotBlank String identifier,

        @Schema(description = "가입 시 등록한 휴대폰 번호 (하이픈 없이)", example = "01012345678")
        @NotBlank String phone
) {}
