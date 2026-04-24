package com.eodigaljido.backend.controller;

import com.eodigaljido.backend.dto.weather.WeatherResponse;
import com.eodigaljido.backend.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
@Tag(name = "Weather", description = "날씨 API — 현재 날씨·대기질·7일 예보를 단일 요청으로 반환")
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping
    @Operation(
            summary = "날씨 통합 조회",
            description = """
                    현재 날씨·대기질·7일 예보를 단일 요청으로 반환합니다. 인증 불필요.

                    ### 사용 방법
                    ```
                    GET /api/weather?location=구미시 상모동
                    GET /api/weather?location=서울 강남구
                    GET /api/weather?location=부산 해운대구
                    ```
                    전국 시·군·구 단위 지원. 읍·면·동 입력 시 상위 시·군으로 자동 매핑됩니다.
                    (예: `구미시 상모동` → 구미시 좌표 사용)

                    ### 캐시 정책
                    - 응답 **10분** 캐시
                    - 기상청 장애 시 최대 1시간 stale 데이터 반환 (`"stale": true` 포함)
                    - 에어코리아 조회 실패 시 `air` 필드는 `null` 반환 (날씨 데이터는 정상 제공)

                    ### 백엔드 내부 동작
                    1. `location` → 격자좌표(nx/ny) 및 측정소명 변환
                    2. 기상청 초단기실황 · 단기예보 · 에어코리아 3개 API **병렬 호출**
                    3. 통합 응답 반환
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "날씨 정보 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "구미시 상모동 예시",
                                    value = """
                                            {
                                              "location": "구미시 상모동",
                                              "fetchedAt": "2025-07-15T14:00:00+09:00",
                                              "current": {
                                                "temperature": 31.2,
                                                "feelsLike": 34.8,
                                                "humidity": 72,
                                                "windSpeed": 2.1,
                                                "windDirection": "남서",
                                                "precipitation1h": 0.0,
                                                "precipitationType": "없음",
                                                "weatherIcon": "sunny",
                                                "weatherDesc": "맑음"
                                              },
                                              "air": {
                                                "pm10": 28,
                                                "pm10Grade": "좋음",
                                                "pm25": 12,
                                                "pm25Grade": "좋음",
                                                "aqi": 42,
                                                "aqiGrade": "좋음"
                                              },
                                              "weekly": [
                                                {
                                                  "date": "2025-07-15",
                                                  "dayOfWeek": "화",
                                                  "weatherIcon": "sunny",
                                                  "weatherDesc": "맑음",
                                                  "tempMax": 33.0,
                                                  "tempMin": 24.5,
                                                  "pop": 10
                                                },
                                                {
                                                  "date": "2025-07-16",
                                                  "dayOfWeek": "수",
                                                  "weatherIcon": "rainy",
                                                  "weatherDesc": "비",
                                                  "tempMax": 29.0,
                                                  "tempMin": 23.0,
                                                  "pop": 70
                                                }
                                              ],
                                              "stale": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "location 파라미터 누락",
                                            value = """
                                                    {
                                                      "code": "MISSING_PARAM",
                                                      "message": "location 파라미터가 필요합니다."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "지원하지 않는 지역",
                                            value = """
                                                    {
                                                      "code": "INVALID_LOCATION",
                                                      "message": "요청한 지역을 찾을 수 없습니다."
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "기상청 API 호출 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "code": "KMA_API_ERROR",
                                              "message": "기상청 API 호출에 실패했습니다."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "503",
                    description = "날씨 서비스 일시 불가 (기상청 점검 등)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "code": "KMA_MAINTENANCE",
                                              "message": "날씨 서비스를 일시적으로 사용할 수 없습니다."
                                            }
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<?> getWeather(
            @Parameter(
                    description = "조회할 지역명. 전국 시·군·구 지원. 읍·면·동 포함 입력 가능.",
                    example = "구미시 상모동",
                    required = true
            )
            @RequestParam(required = false) String location) {

        if (location == null || location.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "code", "MISSING_PARAM",
                    "message", "location 파라미터가 필요합니다."
            ));
        }
        try {
            WeatherResponse response = weatherService.getWeather(location);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "code", "INVALID_LOCATION",
                    "message", "요청한 지역을 찾을 수 없습니다."
            ));
        } catch (RuntimeException e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("KMA_API_ERROR")) {
                return ResponseEntity.status(502).body(Map.of(
                        "code", "KMA_API_ERROR",
                        "message", "기상청 API 호출에 실패했습니다."
                ));
            }
            return ResponseEntity.status(503).body(Map.of(
                    "code", "KMA_MAINTENANCE",
                    "message", "날씨 서비스를 일시적으로 사용할 수 없습니다."
            ));
        }
    }
}
