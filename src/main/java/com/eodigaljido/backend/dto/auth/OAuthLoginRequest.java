package com.eodigaljido.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record OAuthLoginRequest(
        @NotBlank String code
) {}
