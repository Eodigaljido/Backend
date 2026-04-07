package com.eodigaljido.backend.dto.route;

import com.eodigaljido.backend.domain.route.Route;
import com.eodigaljido.backend.domain.route.Route.RouteStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "루트 상세 응답 (경유지 포함)")
public record RouteResponse(
        @Schema(description = "루트 ID (공유 활성화/비활성화 등 ID 기반 API 사용 시 필요)", example = "1")
        Long id,

        @Schema(description = "루트 UUID (외부 식별자)", example = "550e8400-e29b-41d4-a716-446655440000")
        String uuid,

        @Schema(description = "루트 이름", example = "서울 고궁 투어")
        String title,

        @Schema(description = "루트 설명", example = "경복궁, 창덕궁, 창경궁을 잇는 고궁 탐방 루트")
        String description,

        @Schema(description = "상태 (DRAFT | PUBLISHED)", example = "PUBLISHED")
        RouteStatus status,

        @Schema(description = "공유 여부", example = "true")
        boolean isShared,

        @Schema(description = "총 거리 (km)", example = "3.50")
        BigDecimal totalDistance,

        @Schema(description = "예상 소요시간 (분)", example = "120")
        Integer estimatedTime,

        @Schema(description = "대표 이미지 URL", example = "https://example.com/thumbnail.jpg")
        String thumbnailUrl,

        @Schema(description = "경유지 목록 (sequence 오름차순)")
        List<WaypointResponse> waypoints,

        @Schema(description = "생성일시", example = "2026-03-19T10:00:00")
        LocalDateTime createdAt,

        @Schema(description = "최종 수정일시", example = "2026-03-19T12:30:00")
        LocalDateTime updatedAt
) {
    public static RouteResponse of(Route route, List<WaypointResponse> waypoints) {
        return new RouteResponse(
                route.getId(),
                route.getUuid(),
                route.getTitle(),
                route.getDescription(),
                route.getStatus(),
                route.isShared(),
                route.getTotalDistance(),
                route.getEstimatedTime(),
                route.getThumbnailUrl(),
                waypoints,
                route.getCreatedAt(),
                route.getUpdatedAt()
        );
    }
}
