package com.eodigaljido.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "solapi")
public class SolapiProperties {
    private String apiKey;
    private String apiSecret;
    private String sender;
}
