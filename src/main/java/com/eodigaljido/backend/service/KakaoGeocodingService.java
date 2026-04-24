package com.eodigaljido.backend.service;

import com.eodigaljido.backend.config.WeatherProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoGeocodingService {

    private static final String KAKAO_GEOCODE_URL =
            "https://dapi.kakao.com/v2/local/search/address.json";

    private final WeatherProperties weatherProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public record GeoResult(double lat, double lon,
                            String sido,    // 시도 (e.g. 경상북도)
                            String sigungu) // 시군구 (e.g. 구미시)
    {}

    public Optional<GeoResult> geocode(String address) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            String url = KAKAO_GEOCODE_URL + "?query="
                    + URLEncoder.encode(address, StandardCharsets.UTF_8)
                    + "&size=1";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "KakaoAK " + weatherProperties.getKakaoApiKey())
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.warn("카카오 지오코딩 HTTP {}: {}", response.statusCode(), address);
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode documents = root.path("documents");
            if (!documents.isArray() || documents.isEmpty()) {
                log.warn("카카오 지오코딩 결과 없음: {}", address);
                return Optional.empty();
            }

            JsonNode doc = documents.get(0);
            double lat = doc.path("y").asDouble();
            double lon = doc.path("x").asDouble();

            // 도로명주소 우선, 없으면 지번주소
            JsonNode addr = doc.path("road_address");
            if (addr.isMissingNode() || addr.isNull()) {
                addr = doc.path("address");
            }
            String sido    = addr.path("region_1depth_name").asText("");
            String sigungu = addr.path("region_2depth_name").asText("");

            return Optional.of(new GeoResult(lat, lon, sido, sigungu));

        } catch (Exception e) {
            log.error("카카오 지오코딩 실패: address={} error={}", address, e.getMessage());
            return Optional.empty();
        }
    }
}
