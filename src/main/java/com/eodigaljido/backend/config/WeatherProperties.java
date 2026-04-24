package com.eodigaljido.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "weather")
public class WeatherProperties {
    private String kmaApiKey;
    private String airKoreaApiKey;
    private String kakaoApiKey;
}
