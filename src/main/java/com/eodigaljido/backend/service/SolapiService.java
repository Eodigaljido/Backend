package com.eodigaljido.backend.service;

import com.eodigaljido.backend.config.SolapiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SolapiService {

    private final SolapiProperties solapiProperties;

    private static final String SEND_URL = "https://api.solapi.com/messages/v4/send-many/detail";

    public void sendSms(String to, String text) {
        try {
            String authHeader = createAuthHeader();
            String body = buildMessageJson(to, text);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SEND_URL))
                    .header("Authorization", authHeader)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                throw new RuntimeException("SMS 발송 실패: " + response.body());
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("SMS 발송 중 오류 발생", e);
        }
    }

    private String createAuthHeader() throws Exception {
        String dateTime = Instant.now().toString();
        String salt = UUID.randomUUID().toString().replace("-", "");
        String signature = generateSignature(solapiProperties.getApiSecret(), dateTime, salt);
        return "HMAC-SHA256 apiKey=%s, date=%s, salt=%s, signature=%s"
                .formatted(solapiProperties.getApiKey(), dateTime, salt, signature);
    }

    private String generateSignature(String apiSecret, String dateTime, String salt) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(apiSecret.getBytes(), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal((dateTime + salt).getBytes()));
    }

    private String buildMessageJson(String to, String text) {
        String escaped = text.replace("\"", "\\\"");
        return """
                {
                  "messages": [
                    {
                      "to": "%s",
                      "from": "%s",
                      "text": "%s"
                    }
                  ]
                }
                """.formatted(to, solapiProperties.getSender(), escaped);
    }
}
