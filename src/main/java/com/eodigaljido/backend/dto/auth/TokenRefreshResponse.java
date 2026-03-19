package com.eodigaljido.backend.dto.auth;

public record TokenRefreshResponse(
        String accessToken,
        String tokenType
) {
    public static TokenRefreshResponse of(String accessToken) {
        return new TokenRefreshResponse(accessToken, "Bearer");
    }
}
