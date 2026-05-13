package com.eodigaljido.backend.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이동 구간(leg) 요청")
public record LegRequest(
        @Schema(description = "클라이언트 측 임시 ID (서버에서 무시됨)", example = "leg-1")
        String id,

        @Schema(description = "이동 수단 (walk | transit | car | bike)", example = "transit")
        String mode,

        @Schema(description = "소요 시간 (분)", example = "12")
        Integer minutes,

        @Schema(description = "대중교통 종류 (bus | subway | train), transit일 때만 사용", example = "subway")
        String transitType,

        @Schema(description = "경로 요약", example = "2호선 시청역 방면")
        String directionsSummary,

        @Schema(description = "경로 상세", example = "시청역 2호선 탑승 후 3정거장")
        String directionsDetail,

        @Schema(description = "이동 거리 (미터)", example = "950")
        Integer distanceMeters
) {}
