package com.eodigaljido.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청 (전화번호 인증 완료 후 호출)")
public record RegisterRequest(
        @Schema(description = "이메일 주소 (중복 불가)", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Email String email,

        @Schema(description = "비밀번호 (8~100자)", example = "password123!", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(min = 8, max = 100) String password,

        @Schema(description = "닉네임 (2~50자, 중복 불가)", example = "어디갈지몰라", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(min = 2, max = 50) String nickname,

        @Schema(description = "휴대폰 번호 (하이픈 없이, 사전에 /auth/phone/verify로 인증 완료 필요)", example = "01012345678", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Pattern(regexp = "^01[0-9]{8,9}$") String phone
) {}
