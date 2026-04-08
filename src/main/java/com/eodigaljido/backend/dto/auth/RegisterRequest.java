package com.eodigaljido.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "회원가입 요청")
public record RegisterRequest(
        @Schema(description = "사용자 아이디 (8자 이하, 중복 불가)", example = "john123", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 8, message = "아이디는 8자 이하여야 합니다.") String userId,

        @Schema(description = "이메일 주소 (중복 불가)", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Email String email,

        @Schema(description = "비밀번호 (8~100자, 소문자·숫자·특수문자 각 1개 이상 포함)", example = "password1!", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(min = 8, max = 100, message = "비밀번호는 8~100자 사이여야 합니다.")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&#^()\\-_=+\\[\\]{}|;:,.<>?/~`]).{8,}$",
                message = "비밀번호는 소문자, 숫자, 특수문자를 각각 최소 1개 이상 포함해야 합니다."
        )
        String password,

        @Schema(description = "닉네임 (2~50자, 중복 불가)", example = "어디갈지몰라", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(min = 2, max = 50) String nickname
) {}
