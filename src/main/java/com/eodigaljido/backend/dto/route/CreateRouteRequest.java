package com.eodigaljido.backend.dto.route;

import com.eodigaljido.backend.domain.route.Route.RouteStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "루트 생성 요청")
public record CreateRouteRequest(
        @NotBlank
        @Size(max = 100)
        @Schema(description = "루트 이름 (최대 100자)", example = "서울 고궁 투어", requiredMode = Schema.RequiredMode.REQUIRED)
        String title,

        @Schema(description = "루트 설명", example = "경복궁, 창덕궁, 창경궁을 잇는 고궁 탐방 루트")
        String description,

        @Schema(description = "초기 상태. 생략 시 DRAFT로 저장됨 (DRAFT | PUBLISHED)", example = "DRAFT")
        RouteStatus status,

        @Schema(description = "총 거리 (km)", example = "3.50")
        BigDecimal totalDistance,

        @Schema(description = "예상 소요시간 (분)", example = "120")
        Integer estimatedTime,

        @Schema(description = "대표 이미지 URL (최대 512자)", example = "https://example.com/thumbnail.jpg")
        String thumbnailUrl,

        @Valid
        @Schema(description = "경유지 목록 (순서대로 전달)")
        List<WaypointRequest> waypoints
) {}
