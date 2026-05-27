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
    private static final String KAKAO_KEYWORD_URL =
            "https://dapi.kakao.com/v2/local/search/keyword.json";

    private static final long GEO_CACHE_TTL_MS = 24 * 60 * 60 * 1000L; // 24시간
    private static final int KAKAO_RESULT_SIZE = 3;

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

        for (String query : buildAddressQueries(address)) {
            Optional<GeoResult> result = searchAddress(address, query);
            if (result.isPresent()) {
                geoCache.put(address, new CachedGeoResult(result.get(), System.currentTimeMillis()));
                return result;
            }
        }

        for (String query : buildKeywordQueries(address)) {
            Optional<GeoResult> result = searchKeyword(address, query);
            if (result.isPresent()) {
                geoCache.put(address, new CachedGeoResult(result.get(), System.currentTimeMillis()));
                return result;
            }
        }

        log.warn("카카오 지오코딩 최종 결과 없음: original={} addressQueries={} keywordQueries={}",
                address, buildAddressQueries(address), buildKeywordQueries(address));
        return Optional.empty();
    }

    private Optional<GeoResult> searchAddress(String originalAddress, String query) {
        String url = KAKAO_GEOCODE_URL + "?query=" + encode(query) + "&size=" + KAKAO_RESULT_SIZE;
        try {
            JsonNode root = callKakao(url, originalAddress, query, "address");
            JsonNode documents = root.path("documents");
            logKakaoMeta("address", originalAddress, query, root, documents);
            if (!documents.isArray() || documents.isEmpty()) {
                return Optional.empty();
            }

            JsonNode doc = documents.get(0);
            JsonNode addr = doc.path("road_address");
            if (addr.isMissingNode() || addr.isNull()) {
                addr = doc.path("address");
            }
            GeoResult result = toGeoResult(doc, addr);
            log.info("카카오 지오코딩 성공: original={} query={} matched={} lat={} lon={} sido={} sigungu={}",
                    originalAddress, query, summarizeAddressDocument(doc), result.lat(), result.lon(),
                    result.sido(), result.sigungu());
            return Optional.of(result);
        } catch (Exception e) {
            log.warn("카카오 지오코딩 조회 실패: type=address original={} query={} error={}",
                    originalAddress, query, sanitize(e.getMessage()));
            return Optional.empty();
        }
    }

    private Optional<GeoResult> searchKeyword(String originalAddress, String query) {
        String url = KAKAO_KEYWORD_URL + "?query=" + encode(query) + "&size=" + KAKAO_RESULT_SIZE;
        try {
            JsonNode root = callKakao(url, originalAddress, query, "keyword");
            JsonNode documents = root.path("documents");
            logKakaoMeta("keyword", originalAddress, query, root, documents);
            if (!documents.isArray() || documents.isEmpty()) {
                return Optional.empty();
            }

            JsonNode doc = documents.get(0);
            GeoResult result = toGeoResultFromKeyword(doc);
            log.info("카카오 키워드 지오코딩 성공: original={} query={} matched={} lat={} lon={} sido={} sigungu={}",
                    originalAddress, query, summarizeKeywordDocument(doc), result.lat(), result.lon(),
                    result.sido(), result.sigungu());
            return Optional.of(result);
        } catch (Exception e) {
            log.warn("카카오 지오코딩 조회 실패: type=keyword original={} query={} error={}",
                    originalAddress, query, sanitize(e.getMessage()));
            return Optional.empty();
        }
    }

    private JsonNode callKakao(String url, String originalAddress, String query, String type) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "KakaoAK " + weatherProperties.getKakaoApiKey())
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new RuntimeException("HTTP " + response.statusCode()
                    + " body=" + sanitize(response.body())
                    + " original=" + originalAddress
                    + " query=" + query
                    + " type=" + type);
        }
        return objectMapper.readTree(response.body());
    }

    private GeoResult toGeoResult(JsonNode doc, JsonNode addr) {
        double lat = doc.path("y").asDouble();
        double lon = doc.path("x").asDouble();
        String sido = addr.path("region_1depth_name").asText("");
        String sigungu = addr.path("region_2depth_name").asText("");
        return new GeoResult(lat, lon, sido, sigungu);
    }

    private GeoResult toGeoResultFromKeyword(JsonNode doc) {
        double lat = doc.path("y").asDouble();
        double lon = doc.path("x").asDouble();
        String[] regions = extractRegions(doc.path("address_name").asText(""));
        return new GeoResult(lat, lon, regions[0], regions[1]);
    }

    private void logKakaoMeta(String type, String originalAddress, String query, JsonNode root, JsonNode documents) {
        JsonNode meta = root.path("meta");
        int count = documents.isArray() ? documents.size() : 0;
        String first = count > 0
                ? ("address".equals(type) ? summarizeAddressDocument(documents.get(0)) : summarizeKeywordDocument(documents.get(0)))
                : null;
        log.info("카카오 지오코딩 응답: type={} original={} query={} totalCount={} pageableCount={} resultCount={} first={}",
                type,
                originalAddress,
                query,
                meta.path("total_count").asInt(-1),
                meta.path("pageable_count").asInt(-1),
                count,
                first);
    }

    private java.util.List<String> buildAddressQueries(String address) {
        java.util.LinkedHashSet<String> queries = new java.util.LinkedHashSet<>();
        String normalized = normalizeSpaces(address);
        if (!normalized.isBlank()) {
            queries.add(normalized);
            queries.add(reorderKoreanAddress(normalized));
            queries.add(removeTrailingNumberOnly(normalized));
        }
        queries.removeIf(String::isBlank);
        return java.util.List.copyOf(queries);
    }

    private java.util.List<String> buildKeywordQueries(String address) {
        java.util.LinkedHashSet<String> queries = new java.util.LinkedHashSet<>();
        String normalized = normalizeSpaces(address);
        if (!normalized.isBlank()) {
            queries.add(normalized);
            queries.add(removeTrailingNumberOnly(normalized));
        }
        queries.removeIf(String::isBlank);
        return java.util.List.copyOf(queries);
    }

    private String reorderKoreanAddress(String address) {
        String[] parts = address.split(" ");
        if (parts.length < 4) {
            return address;
        }

        int sigunguIndex = -1;
        int townIndex = -1;
        for (int i = 1; i < parts.length; i++) {
            if (sigunguIndex < 0 && parts[i].matches(".+(시|군|구)$")) {
                sigunguIndex = i;
            }
            if (townIndex < 0 && parts[i].matches(".+(읍|면|동)$")) {
                townIndex = i;
            }
        }

        if (townIndex > 0 && sigunguIndex > townIndex) {
            java.util.List<String> reordered = new java.util.ArrayList<>(java.util.List.of(parts));
            String sigungu = reordered.remove(sigunguIndex);
            reordered.add(townIndex, sigungu);
            return String.join(" ", reordered);
        }
        return address;
    }

    private String removeTrailingNumberOnly(String address) {
        return normalizeSpaces(address.replaceFirst("\\s+\\d+(-\\d+)?$", ""));
    }

    private String summarizeAddressDocument(JsonNode doc) {
        String road = doc.path("road_address").path("address_name").asText("");
        String jibun = doc.path("address").path("address_name").asText("");
        return sanitize("road=" + road + ", jibun=" + jibun);
    }

    private String summarizeKeywordDocument(JsonNode doc) {
        return sanitize("place=" + doc.path("place_name").asText("")
                + ", road=" + doc.path("road_address_name").asText("")
                + ", address=" + doc.path("address_name").asText(""));
    }

    private String normalizeSpaces(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private String[] extractRegions(String addressName) {
        String[] parts = normalizeSpaces(addressName).split(" ");
        String sido = parts.length > 0 ? parts[0] : "";
        String sigungu = "";
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].matches(".+(시|군|구)$")) {
                sigungu = parts[i];
                break;
            }
        }
        return new String[]{sido, sigungu};
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String sanitized = value.replaceAll("[\\r\\n\\t]+", " ");
        return sanitized.length() > 300 ? sanitized.substring(0, 300) + "..." : sanitized;
    }
}
