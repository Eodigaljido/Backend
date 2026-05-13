package com.eodigaljido.backend.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;

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

        @Schema(description = "위도", example = "37.5796212", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal lat,

        @Schema(description = "경도", example = "126.9770162", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal lng
) {}
