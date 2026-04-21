package com.eodigaljido.backend.dto.route;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "루트 수정 요청 (경유지는 전체 교체)")
public record UpdateRouteRequest(
        @NotBlank
        @Size(max = 100)
        @Schema(description = "루트 이름 (최대 100자)", example = "서울 고궁 투어 (수정)", requiredMode = Schema.RequiredMode.REQUIRED)
        String title,

        @Schema(description = "루트 설명", example = "수정된 고궁 탐방 루트 설명")
        String description,

        @Schema(description = "총 거리 (km)", example = "4.20")
        BigDecimal totalDistance,

        @Schema(description = "예상 소요시간 (분)", example = "150")
        Integer estimatedTime,

        @Schema(description = "대표 이미지 URL (최대 512자)", example = "https://example.com/new-thumbnail.jpg")
        String thumbnailUrl,

        @Schema(description = "지역 태그 (온보딩 매칭용)", example = "서울")
        String region,

        @Schema(description = "활동 유형 태그 (온보딩 매칭용)", example = "관광")
        String activityType,

        @Valid
        @Schema(description = "경유지 목록 — 기존 경유지를 전체 삭제하고 새 목록으로 교체합니다")
        List<WaypointRequest> waypoints
) {}
