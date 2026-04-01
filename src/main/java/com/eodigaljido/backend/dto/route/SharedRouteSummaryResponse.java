package com.eodigaljido.backend.dto.route;

import com.eodigaljido.backend.domain.route.Route.RouteStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "공유된 루트 목록 응답")
public record SharedRouteSummaryResponse(
        @Schema(description = "루트 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        String uuid,

        @Schema(description = "루트 이름", example = "서울 고궁 투어")
        String title,

        @Schema(description = "루트 설명", example = "경복궁, 창덕궁, 창경궁을 잇는 고궁 탐방 루트")
        String description,

        @Schema(description = "상태 (DRAFT | PUBLISHED)", example = "PUBLISHED")
        RouteStatus status,

        @Schema(description = "총 거리 (km)", example = "3.5")
        BigDecimal totalDistance,

        @Schema(description = "예상 소요시간 (분)", example = "120")
        Integer estimatedTime,

        @Schema(description = "대표 이미지 URL", example = "https://example.com/thumbnail.jpg")
        String thumbnailUrl,

        @Schema(description = "첫 번째 경유지 이름", example = "경복궁")
        String name,

        @Schema(description = "루트 작성자 UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String authorUuid
) {}
