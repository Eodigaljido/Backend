package com.eodigaljido.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties {

    private Provider google = new Provider();
    private Provider kakao = new Provider();

    @Getter
    @Setter
    public static class Provider {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        private List<String> allowedRedirectUris = new ArrayList<>();
    }
}
