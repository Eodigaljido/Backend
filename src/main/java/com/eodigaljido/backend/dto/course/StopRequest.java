package com.eodigaljido.backend.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "경유지(stop) 요청")
public record StopRequest(
        @Schema(description = "클라이언트 측 임시 ID (서버에서 무시됨)", example = "stop-1")
        String id,

        @Schema(description = "경유지 구분 (start | via | end)", example = "start")
        String kind,

        @Schema(description = "경유지 이름", example = "경복궁", requiredMode = Schema.RequiredMode.REQUIRED)
        String title,

        @Schema(description = "예상 도착/출발 시각 등 일정 문자열", example = "09:00")
        String timeLine,

        @NotNull(message = "경유지 위도는 필수입니다.")
        @DecimalMin(value = "-90.0", message = "경유지 위도는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90.0", message = "경유지 위도는 90 이하여야 합니다.")
        @Schema(description = "위도", example = "37.5796212", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal lat,

        @NotNull(message = "경유지 경도는 필수입니다.")
        @DecimalMin(value = "-180.0", message = "경유지 경도는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180.0", message = "경유지 경도는 180 이하여야 합니다.")
        @Schema(description = "경도", example = "126.9770162", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal lng
) {}
