package com.eodigaljido.backend.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "전화번호 변경 요청 (사전에 CHANGE_PHONE purpose로 인증 완료 필요)")
public record UpdatePhoneRequest(
        @Schema(description = "변경할 휴대폰 번호 (하이픈 없이, /auth/phone/verify CHANGE_PHONE 인증 완료 필요)", example = "01098765432", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Pattern(regexp = "^01[0-9]{8,9}$") String phone
) {}
