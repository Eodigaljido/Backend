package com.eodigaljido.backend.dto.auth;

import com.eodigaljido.backend.domain.user.PhoneVerification;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(description = "SMS 인증번호 발송 요청")
public record PhoneCodeRequest(
        @Schema(description = "인증번호를 받을 휴대폰 번호 (하이픈 없이)", example = "01012345678", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Pattern(regexp = "^01[0-9]{8,9}$") String phone,

        @Schema(
                description = "인증 목적. REGISTER: 회원가입용, CHANGE_PHONE: 전화번호 변경용",
                example = "REGISTER",
                allowableValues = {"REGISTER", "CHANGE_PHONE"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull PhoneVerification.Purpose purpose
) {}
