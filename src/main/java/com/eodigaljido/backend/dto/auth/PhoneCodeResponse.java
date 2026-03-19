package com.eodigaljido.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "SMS 인증번호 발송 응답")
public record PhoneCodeResponse(
        @Schema(description = "인증번호 유효 시간(초). 이 시간 내에 /auth/phone/verify를 호출해야 함", example = "180")
        int expiresInSeconds
) {
    public static PhoneCodeResponse of(int expiresInSeconds) {
        return new PhoneCodeResponse(expiresInSeconds);
    }
}
