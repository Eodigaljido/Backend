package com.eodigaljido.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai")
public class AiChatProperties {

    private Gemini gemini = new Gemini();
    private Session session = new Session();

    @Getter
    @Setter
    public static class Gemini {
        private String apiKey = "";
        private String model = "gemini-2.0-flash";
        private int maxTokens = 4096;
        private String baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai/";
    }

    @Getter
    @Setter
    public static class Session {
        private int ttlSeconds = 3600;
        private int maxMessages = 20;
    }
}
