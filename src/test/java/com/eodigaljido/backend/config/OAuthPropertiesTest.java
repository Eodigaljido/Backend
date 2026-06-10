package com.eodigaljido.backend.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthPropertiesTest {

    @Test
    void acceptsConfiguredAndAdditionalRedirectUris() {
        OAuthProperties.Provider provider = new OAuthProperties.Provider();
        provider.setAllowedRedirectUris(List.of("https://api.eodigaljido.uk/auth/oauth/google"));
        provider.setAdditionalAllowedRedirectUris(List.of("https://eodigaljido.uk/oauth/google"));

        assertThat(provider.isRedirectUriAllowed("https://api.eodigaljido.uk/auth/oauth/google")).isTrue();
        assertThat(provider.isRedirectUriAllowed("https://eodigaljido.uk/oauth/google")).isTrue();
        assertThat(provider.isRedirectUriAllowed("https://evil.example/oauth/google")).isFalse();
    }
}
