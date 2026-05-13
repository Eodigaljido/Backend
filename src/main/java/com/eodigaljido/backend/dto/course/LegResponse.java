package com.eodigaljido.backend.dto.course;

import com.eodigaljido.backend.domain.route.RouteLeg;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이동 구간(leg) 응답")
public record LegResponse(
        @Schema(description = "구간 순서 (1부터 시작)", example = "1")
        int sequence,

        @Schema(description = "이동 수단 (walk | transit | car | bike)", example = "transit")
        String mode,

        @Schema(description = "소요 시간 (분)", example = "12")
        Integer minutes,

        @Schema(description = "대중교통 종류 (bus | subway | train)", example = "subway")
        String transitType,

        @Schema(description = "경로 요약", example = "2호선 시청역 방면")
        String directionsSummary,

        @Schema(description = "경로 상세", example = "시청역 2호선 탑승 후 3정거장")
        String directionsDetail,

        @Schema(description = "이동 거리 (미터)", example = "950")
        Integer distanceMeters
) {
    public static LegResponse from(RouteLeg l) {
        return new LegResponse(l.getSequence(), l.getMode(), l.getMinutes(), l.getTransitType(),
                l.getDirectionsSummary(), l.getDirectionsDetail(), l.getDistanceMeters());
    }
}
