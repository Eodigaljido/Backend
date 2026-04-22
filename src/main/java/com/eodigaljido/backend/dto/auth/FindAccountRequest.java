package com.eodigaljido.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "아이디/이메일 찾기 — 인증번호 발송 요청")
public record FindAccountRequest(
        @Schema(description = "인증번호를 받을 휴대폰 번호 (하이픈 없이)", example = "01012345678")
        @NotBlank String phone
) {}
