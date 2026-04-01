package com.eodigaljido.backend.dto.route;

import com.eodigaljido.backend.domain.route.Route;
import com.eodigaljido.backend.domain.route.Route.RouteStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "루트 요약 응답 (목록 조회용, 경유지 미포함)")
public record RouteSummaryResponse(
        @Schema(description = "루트 UUID (외부 식별자)", example = "550e8400-e29b-41d4-a716-446655440000")
        String uuid,

        @Schema(description = "루트 이름", example = "서울 고궁 투어")
        String title,

        @Schema(description = "상태 (DRAFT | PUBLISHED)", example = "DRAFT")
        RouteStatus status,

        @Schema(description = "공유 여부", example = "false")
        boolean isShared,

        @Schema(description = "총 거리 (km)", example = "3.50")
        BigDecimal totalDistance,

        @Schema(description = "예상 소요시간 (분)", example = "120")
        Integer estimatedTime,

        @Schema(description = "대표 이미지 URL", example = "https://example.com/thumbnail.jpg")
        String thumbnailUrl,

        @Schema(description = "생성일시", example = "2026-03-19T10:00:00")
        LocalDateTime createdAt
) {
    public static RouteSummaryResponse from(Route route) {
        return new RouteSummaryResponse(
                route.getUuid(),
                route.getTitle(),
                route.getStatus(),
                route.isShared(),
                route.getTotalDistance(),
                route.getEstimatedTime(),
                route.getThumbnailUrl(),
                route.getCreatedAt()
        );
    }
}
