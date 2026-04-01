package com.eodigaljido.backend.dto.route;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "경유지 요청")
public record WaypointRequest(
        @NotNull
        @Schema(description = "경유지 순서 (1부터 시작)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer sequence,

        @Schema(description = "지점 이름", example = "경복궁")
        String name,

        @NotNull
        @Schema(description = "위도", example = "37.5796212", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal latitude,

        @NotNull
        @Schema(description = "경도", example = "126.9770162", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal longitude,

        @Schema(description = "주소", example = "서울특별시 종로구 사직로 161")
        String address,

        @Schema(description = "메모", example = "정문 쪽에서 진입")
        String memo
) {}
