package com.eodigaljido.backend.security;

import com.eodigaljido.backend.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-that-is-at-least-thirty-two-bytes-long");
        properties.setAccessTokenExpiry(60_000);
        properties.setRefreshTokenExpiry(60_000);
        jwtTokenProvider = new JwtTokenProvider(properties);
    }

    @Test
    void acceptsOnlyAccessTokensForAuthentication() {
        assertThat(jwtTokenProvider.isValidAccessToken(
                jwtTokenProvider.generateAccessToken(1L, "USER"))).isTrue();
        assertThat(jwtTokenProvider.isValidAccessToken(
                jwtTokenProvider.generateRefreshToken(1L))).isFalse();
        assertThat(jwtTokenProvider.isValidAccessToken("invalid-token")).isFalse();
    }

    @Test
    void distinguishesExpiredInvalidAndWrongTypeAccessTokens() {
        String refreshToken = jwtTokenProvider.generateRefreshToken(1L);

        assertThat(jwtTokenProvider.validateAccessToken(null).status())
                .isEqualTo(JwtTokenProvider.AccessTokenStatus.MISSING);
        assertThat(jwtTokenProvider.validateAccessToken("invalid-token").status())
                .isEqualTo(JwtTokenProvider.AccessTokenStatus.INVALID);
        assertThat(jwtTokenProvider.validateAccessToken(refreshToken).status())
                .isEqualTo(JwtTokenProvider.AccessTokenStatus.WRONG_TYPE);
        assertThat(jwtTokenProvider.isValidRefreshToken(refreshToken)).isTrue();
        assertThat(jwtTokenProvider.isValidRefreshToken(
                jwtTokenProvider.generateAccessToken(1L, "USER"))).isFalse();
    }
}
