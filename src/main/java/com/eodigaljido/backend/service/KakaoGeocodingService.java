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
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoGeocodingService {

    private static final String KAKAO_GEOCODE_URL =
            "https://dapi.kakao.com/v2/local/search/address.json";

    private static final long GEO_CACHE_TTL_MS = 24 * 60 * 60 * 1000L; // 24시간

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final WeatherProperties weatherProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, CachedGeoResult> geoCache = new ConcurrentHashMap<>();
    private record CachedGeoResult(GeoResult data, long timestamp) {}

    public record GeoResult(double lat, double lon,
                            String sido,
                            String sigungu) {}

    public Optional<GeoResult> geocode(String address) {
        // 캐시 확인
        CachedGeoResult cached = geoCache.get(address);
        if (cached != null && System.currentTimeMillis() - cached.timestamp() < GEO_CACHE_TTL_MS) {
            log.debug("지오코딩 캐시 히트: {}", address);
            return Optional.of(cached.data());
        }

        try {
            String url = KAKAO_GEOCODE_URL + "?query="
                    + URLEncoder.encode(address, StandardCharsets.UTF_8)
                    + "&size=1";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "KakaoAK " + weatherProperties.getKakaoApiKey())
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request,
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

            JsonNode addr = doc.path("road_address");
            if (addr.isMissingNode() || addr.isNull()) {
                addr = doc.path("address");
            }
            String sido    = addr.path("region_1depth_name").asText("");
            String sigungu = addr.path("region_2depth_name").asText("");

            GeoResult result = new GeoResult(lat, lon, sido, sigungu);
            geoCache.put(address, new CachedGeoResult(result, System.currentTimeMillis()));
            return Optional.of(result);

        } catch (Exception e) {
            log.error("카카오 지오코딩 실패: address={} error={}", address, e.getMessage());
            return Optional.empty();
        }
    }
}
