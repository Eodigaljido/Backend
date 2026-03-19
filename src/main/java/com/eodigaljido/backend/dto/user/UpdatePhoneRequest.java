package com.eodigaljido.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdatePhoneRequest(
        @NotBlank @Pattern(regexp = "^01[0-9]{8,9}$") String phone
) {}
