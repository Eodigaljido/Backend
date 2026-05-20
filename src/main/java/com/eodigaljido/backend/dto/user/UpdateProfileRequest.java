package com.eodigaljido.backend.dto.user;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 2, max = 14, message = "닉네임은 2자 이상 14자 이하여야 합니다.")
        String nickname,

        @Size(max = 255, message = "자기소개는 255자 이하여야 합니다.")
        String bio
) {}
