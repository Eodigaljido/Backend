package com.eodigaljido.backend.dto.auth;

import com.eodigaljido.backend.domain.user.PhoneVerification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PhoneCodeRequest(
        @NotBlank @Pattern(regexp = "^01[0-9]{8,9}$") String phone,
        @NotNull PhoneVerification.Purpose purpose
) {}
