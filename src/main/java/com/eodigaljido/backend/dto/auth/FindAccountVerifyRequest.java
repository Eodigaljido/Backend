package com.eodigaljido.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "아이디/이메일 찾기 — 인증번호 확인 요청")
public record FindAccountVerifyRequest(
        @Schema(description = "휴대폰 번호 (하이픈 없이)", example = "01012345678")
        @NotBlank String phone,

        @Schema(description = "6자리 인증번호", example = "123456")
        @NotBlank @Size(min = 6, max = 6) String code
) {}
