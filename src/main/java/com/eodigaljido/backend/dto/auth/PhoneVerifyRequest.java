package com.eodigaljido.backend.dto.auth;

import com.eodigaljido.backend.domain.user.PhoneVerification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PhoneVerifyRequest(
        @NotBlank @Pattern(regexp = "^01[0-9]{8,9}$") String phone,
        @NotBlank @Size(min = 6, max = 6) String code,
        @NotNull PhoneVerification.Purpose purpose
) {}
