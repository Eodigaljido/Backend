package com.eodigaljido.backend.dto.auth;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType
) {
    public static LoginResponse of(String accessToken, String refreshToken) {
        return new LoginResponse(accessToken, refreshToken, "Bearer");
    }
}
