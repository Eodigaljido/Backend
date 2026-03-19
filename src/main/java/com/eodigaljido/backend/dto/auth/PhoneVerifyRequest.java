package com.eodigaljido.backend.dto.auth;

import com.eodigaljido.backend.domain.user.PhoneVerification;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "SMS 인증번호 검증 요청")
public record PhoneVerifyRequest(
        @Schema(description = "인증번호를 받은 휴대폰 번호 (하이픈 없이)", example = "01012345678", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Pattern(regexp = "^01[0-9]{8,9}$") String phone,

        @Schema(description = "SMS로 수신한 6자리 인증번호", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(min = 6, max = 6) String code,

        @Schema(
                description = "인증 목적. 인증번호 발송 시 사용한 purpose와 동일하게 입력",
                example = "REGISTER",
                allowableValues = {"REGISTER", "CHANGE_PHONE"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull PhoneVerification.Purpose purpose
) {}
