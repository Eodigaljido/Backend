package com.eodigaljido.backend.service;

import com.eodigaljido.backend.config.WeatherProperties;
import com.eodigaljido.backend.domain.weather.LocationInfo;
import com.eodigaljido.backend.dto.weather.*;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private final WeatherProperties weatherProperties;
    private final KakaoGeocodingService kakaoGeocodingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String KMA_BASE = "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0";
    private static final String AIR_BASE = "https://apis.data.go.kr/B552584/ArpltnInforInqireSvc";

    private static final long CACHE_TTL_MS   = 30 * 60 * 1000L;       // 30분
    private static final long STALE_MAX_MS   = 3 * 60 * 60 * 1000L;   // 3시간

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final Map<String, CachedWeather> cache = new ConcurrentHashMap<>();

    private record CachedWeather(WeatherResponse data, long timestamp) {}

    public WeatherResponse getWeather(String location) {
        CachedWeather cached = cache.get(location);
        long now = System.currentTimeMillis();

        if (cached != null && now - cached.timestamp() < CACHE_TTL_MS) {
            return cached.data();
        }

        LocationInfo info = resolveLocation(location);

        try {
            WeatherResponse response = fetchAndBuild(location, info);
            cache.put(location, new CachedWeather(response, now));
            return response;
        } catch (Exception e) {
            if (cached != null && now - cached.timestamp() < STALE_MAX_MS) {
                return withStaleFlag(cached.data());
            }
            throw new RuntimeException("KMA_API_ERROR: " + e.getMessage(), e);
        }
    }

    private LocationInfo resolveLocation(String location) {
        KakaoGeocodingService.GeoResult geo = kakaoGeocodingService.geocode(location)
                .orElseThrow(() -> new IllegalArgumentException("INVALID_LOCATION"));

        int[] grid = GeoGridConverter.convert(geo.lat(), geo.lon());
        log.info("지역 좌표 변환: location={} lat={} lon={} nx={} ny={} sido={} sigungu={}",
                location, geo.lat(), geo.lon(), grid[0], grid[1], geo.sido(), geo.sigungu());

        return new LocationInfo(grid[0], grid[1], geo.sido(), geo.sigungu());
    }

    // ── API 호출 ──────────────────────────────────────────────────────────────

    private WeatherResponse fetchAndBuild(String location, LocationInfo info) throws Exception {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));

        CompletableFuture<JsonNode> ncstFuture = CompletableFuture.supplyAsync(
                () -> callNcst(HTTP_CLIENT, info, now));
        CompletableFuture<JsonNode> fcstFuture = CompletableFuture.supplyAsync(
                () -> callFcst(HTTP_CLIENT, info, now));
        CompletableFuture<JsonNode> airFuture = CompletableFuture.supplyAsync(
                () -> callAir(HTTP_CLIENT, info.sido(), info.sigungu()))
                .exceptionally(ex -> {
                    log.warn("에어코리아 조회 최종 실패 (sido={} sigungu={}): {}",
                            info.sido(), info.sigungu(), ex.getMessage());
                    return null;
                });

        JsonNode ncstData = ncstFuture.get(8, TimeUnit.SECONDS);
        JsonNode fcstData = fcstFuture.get(10, TimeUnit.SECONDS);
        JsonNode airData  = airFuture.get(5, TimeUnit.SECONDS);

        List<DailyForecastDto> weekly = parseFcst(fcstData);
        int todayPop = weekly.isEmpty() ? 0 : weekly.get(0).pop();
        CurrentWeatherDto current = parseNcst(ncstData, todayPop);
        AirQualityDto air = airData != null ? parseAir(airData)
                : new AirQualityDto(0, "알수없음", 0, "알수없음", 0, "알수없음");

        String fetchedAt = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return new WeatherResponse(location, fetchedAt, current, air, weekly, null);
    }

    private JsonNode callNcst(HttpClient client, LocationInfo info, ZonedDateTime now) {
        ZonedDateTime base = now.getMinute() < 40 ? now.minusHours(1) : now;
        String baseDate = base.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTime = String.format("%02d00", base.getHour());

        String url = KMA_BASE + "/getUltraSrtNcst"
                + "?serviceKey=" + encode(weatherProperties.getKmaApiKey())
                + "&numOfRows=10&pageNo=1&dataType=JSON"
                + "&base_date=" + baseDate
                + "&base_time=" + baseTime
                + "&nx=" + info.nx()
                + "&ny=" + info.ny();
        return httpGet(client, url);
    }

    private JsonNode callFcst(HttpClient client, LocationInfo info, ZonedDateTime now) {
        String[] baseDt = fcstBaseDateTime(now);
        String url = KMA_BASE + "/getVilageFcst"
                + "?serviceKey=" + encode(weatherProperties.getKmaApiKey())
                + "&numOfRows=1000&pageNo=1&dataType=JSON"
                + "&base_date=" + baseDt[0]
                + "&base_time=" + baseDt[1]
                + "&nx=" + info.nx()
                + "&ny=" + info.ny();
        return httpGet(client, url);
    }

    private JsonNode callAir(HttpClient client, String sido, String sigungu) {
        // 1. 시도별 조회 (가장 안정적 — 측정소명 추측 불필요)
        String sidoAbbr = toAirKoreaSido(sido);
        if (!sidoAbbr.isBlank()) {
            try {
                JsonNode result = callAirBySido(client, sidoAbbr);
                log.info("에어코리아 시도별 조회 성공: sido={}", sidoAbbr);
                return result;
            } catch (Exception e) {
                log.warn("에어코리아 시도별 조회 실패 (sido={}): {}", sidoAbbr, e.getMessage());
            }
        }

        // 2. 측정소별 폴백 (시도 매핑 실패 시)
        for (String candidate : new String[]{sigungu, sigungu.replaceAll("[시군구]$", "")}) {
            if (candidate.isBlank()) continue;
            try {
                JsonNode result = callAirByStation(client, candidate);
                log.info("에어코리아 측정소 조회 성공: station={}", candidate);
                return result;
            } catch (Exception e) {
                log.warn("에어코리아 측정소 조회 실패 (station={}): {}", candidate, e.getMessage());
            }
        }

        throw new RuntimeException("에어코리아 모든 조회 실패: sido=" + sido + ", sigungu=" + sigungu);
    }

    private JsonNode callAirByStation(HttpClient client, String station) {
        String url = AIR_BASE + "/getMsrstnAcctoRltmMesureDnsty"
                + "?serviceKey=" + encode(weatherProperties.getAirKoreaApiKey())
                + "&returnType=json&numOfRows=1&pageNo=1"
                + "&stationName=" + encode(station)
                + "&dataTerm=DAILY&ver=1.0";
        return httpGet(client, url);
    }

    private JsonNode callAirBySido(HttpClient client, String sidoAbbr) {
        String url = AIR_BASE + "/getCtprvnRltmMesureDnsty"
                + "?serviceKey=" + encode(weatherProperties.getAirKoreaApiKey())
                + "&returnType=json&numOfRows=1&pageNo=1"
                + "&sidoName=" + encode(sidoAbbr)
                + "&ver=1.0";
        return httpGet(client, url);
    }

    private String toAirKoreaSido(String sido) {
        return switch (sido) {
            // 전체 이름 → 약어
            case "서울특별시"                 -> "서울";
            case "부산광역시"                 -> "부산";
            case "대구광역시"                 -> "대구";
            case "인천광역시"                 -> "인천";
            case "광주광역시"                 -> "광주";
            case "대전광역시"                 -> "대전";
            case "울산광역시"                 -> "울산";
            case "세종특별자치시"             -> "세종";
            case "경기도"                     -> "경기";
            case "강원도", "강원특별자치도"   -> "강원";
            case "충청북도"                   -> "충북";
            case "충청남도"                   -> "충남";
            case "전라북도", "전북특별자치도" -> "전북";
            case "전라남도"                   -> "전남";
            case "경상북도"                   -> "경북";
            case "경상남도"                   -> "경남";
            case "제주특별자치도"             -> "제주";
            // Kakao가 이미 약어로 반환하는 경우 그대로 사용
            case "서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종",
                 "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주" -> sido;
            default -> "";
        };
    }

    private JsonNode httpGet(HttpClient client, String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new RuntimeException("HTTP " + response.statusCode()
                        + " body=" + response.body());
            }
            String body = response.body();
            if (body.startsWith("<")
                    || (body.contains("\"resultCode\":\"") && !body.contains("\"resultCode\":\"00\""))) {
                throw new RuntimeException("API error body=" + body);
            }
            return objectMapper.readTree(body);
        } catch (Exception e) {
            String safeUrl = url.replaceAll("(authKey|serviceKey)=[^&]+", "$1=***");
            log.error("httpGet 실패: url={} error={}", safeUrl, e.getMessage());
            throw new RuntimeException("httpGet 실패: " + e.getMessage(), e);
        }
    }

    // ── 파싱 ──────────────────────────────────────────────────────────────────

    private CurrentWeatherDto parseNcst(JsonNode root, int pop) {
        Map<String, String> items = new HashMap<>();
        for (JsonNode item : root.path("response").path("body").path("items").path("item")) {
            items.put(item.path("category").asText(), item.path("obsrValue").asText());
        }

        double temp      = toDouble(items.get("T1H"), 0.0);
        int    humidity  = toInt(items.get("REH"), 0);
        double windSpeed = toDouble(items.get("WSD"), 0.0);
        int    vec       = toInt(items.get("VEC"), 0);
        double rn1       = toDouble(items.get("RN1"), 0.0);
        int    pty       = toInt(items.get("PTY"), 0);

        return new CurrentWeatherDto(
                temp,
                feelsLike(temp, windSpeed, humidity),
                humidity,
                windSpeed,
                windDirFromDeg(vec),
                rn1,
                precipType(pty),
                icon(pty, 1),
                desc(pty, 1),
                pop
        );
    }

    private AirQualityDto parseAir(JsonNode root) {
        JsonNode items = root.path("response").path("body").path("items");
        JsonNode item;
        if (items.isArray()) {
            item = items.path(0);              // getMsrstnAcctoRltmMesureDnsty: items=[{...}]
        } else {
            item = items.path("item").path(0); // getCtprvnRltmMesureDnsty: items={item:[{...}]}
        }
        return new AirQualityDto(
                toIntSafe(item.path("pm10Value").asText()),
                grade(item.path("pm10Grade").asText()),
                toIntSafe(item.path("pm25Value").asText()),
                grade(item.path("pm25Grade").asText()),
                toIntSafe(item.path("khaiValue").asText()),
                grade(item.path("khaiGrade").asText())
        );
    }

    private List<DailyForecastDto> parseFcst(JsonNode root) {
        Map<String, DailyData> dailyMap = new LinkedHashMap<>();

        for (JsonNode item : root.path("response").path("body").path("items").path("item")) {
            String category = item.path("category").asText();
            String fcstDate = item.path("fcstDate").asText();
            String fcstTime = item.path("fcstTime").asText();
            String value    = item.path("fcstValue").asText();

            DailyData d = dailyMap.computeIfAbsent(fcstDate, k -> new DailyData());
            switch (category) {
                case "TMX" -> d.tempMax = toDouble(value, d.tempMax);
                case "TMN" -> d.tempMin = toDouble(value, d.tempMin);
                case "POP" -> d.popMax  = Math.max(d.popMax, toInt(value, 0));
                case "SKY" -> { if ("1200".equals(fcstTime)) d.skyNoon = toInt(value, 1); }
                case "PTY" -> { if ("1200".equals(fcstTime)) d.ptyNoon = toInt(value, 0); }
            }
        }

        List<DailyForecastDto> result = new ArrayList<>();
        for (Map.Entry<String, DailyData> e : dailyMap.entrySet()) {
            if (result.size() >= 7) break;
            String dateStr = e.getKey();
            DailyData d = e.getValue();
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
            result.add(new DailyForecastDto(
                    date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    dow(date),
                    icon(d.ptyNoon, d.skyNoon),
                    desc(d.ptyNoon, d.skyNoon),
                    d.tempMax == -Double.MAX_VALUE ? null : d.tempMax,
                    d.tempMin ==  Double.MAX_VALUE ? null : d.tempMin,
                    d.popMax
            ));
        }
        return result;
    }

    // ── 유틸 ──────────────────────────────────────────────────────────────────

    private static class DailyData {
        double tempMax = -Double.MAX_VALUE;
        double tempMin =  Double.MAX_VALUE;
        int popMax  = 0;
        int skyNoon = 1;
        int ptyNoon = 0;
    }

    private String[] fcstBaseDateTime(ZonedDateTime now) {
        int[] bases = {2, 5, 8, 11, 14, 17, 20, 23};
        int h = now.getHour();
        int m = now.getMinute();

        if (h < 2 || (h == 2 && m < 10)) {
            return new String[]{
                    now.minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                    "2300"
            };
        }

        int selected = bases[0];
        for (int b : bases) {
            if (h > b || (h == b && m >= 10)) selected = b;
        }
        return new String[]{
                now.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                String.format("%02d00", selected)
        };
    }

    private String precipType(int pty) {
        return switch (pty) {
            case 1 -> "비";
            case 2 -> "비+눈";
            case 3 -> "눈";
            case 4 -> "소나기";
            default -> "없음";
        };
    }

    private String icon(int pty, int sky) {
        return switch (pty) {
            case 1 -> "rainy";
            case 2 -> "sleet";
            case 3 -> "snowy";
            case 4 -> "shower";
            default -> switch (sky) {
                case 3 -> "partly_cloudy";
                case 4 -> "cloudy";
                default -> "sunny";
            };
        };
    }

    private String desc(int pty, int sky) {
        return switch (pty) {
            case 1 -> "비";
            case 2 -> "비+눈";
            case 3 -> "눈";
            case 4 -> "소나기";
            default -> switch (sky) {
                case 3 -> "구름많음";
                case 4 -> "흐림";
                default -> "맑음";
            };
        };
    }

    private String windDirFromDeg(int deg) {
        String[] dirs = {"북", "북동", "동", "남동", "남", "남서", "서", "북서"};
        return dirs[(int) Math.round(deg / 45.0) % 8];
    }

    private double feelsLike(double temp, double wind, int humidity) {
        if (temp <= 10 && wind >= 1.3) {
            double v = Math.pow(wind, 0.16);
            return Math.round((13.12 + 0.6215 * temp - 11.37 * v + 0.3965 * temp * v) * 10) / 10.0;
        }
        if (temp >= 25 && humidity >= 40) {
            double t = temp, h = humidity;
            double hi = -8.78 + 1.611 * t + 2.339 * h - 0.1461 * t * h
                    - 0.01231 * t * t - 0.01642 * h * h
                    + 0.002211 * t * t * h + 0.000725 * t * h * h
                    - 3.58e-6 * t * t * h * h;
            return Math.round(hi * 10) / 10.0;
        }
        return temp;
    }

    private String grade(String code) {
        return switch (code) {
            case "1" -> "좋음";
            case "2" -> "보통";
            case "3" -> "나쁨";
            case "4" -> "매우나쁨";
            default  -> "알수없음";
        };
    }

    private String dow(LocalDate date) {
        return new String[]{"월", "화", "수", "목", "금", "토", "일"}[date.getDayOfWeek().getValue() - 1];
    }

    private double toDouble(String s, double def) {
        if (s == null || s.isBlank()) return def;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return def; }
    }

    private int toInt(String s, int def) {
        if (s == null || s.isBlank()) return def;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; }
    }

    private int toIntSafe(String s) { return toInt(s, 0); }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private WeatherResponse withStaleFlag(WeatherResponse r) {
        return new WeatherResponse(r.location(), r.fetchedAt(), r.current(), r.air(), r.weekly(), true);
    }
}
