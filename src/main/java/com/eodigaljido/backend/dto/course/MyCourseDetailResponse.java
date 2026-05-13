package com.eodigaljido.backend.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "내 루트 상세 응답 (stops + legs 포함)")
public record MyCourseDetailResponse(
        @Schema(description = "루트 UUID (생성 ID)", example = "550e8400-e29b-41d4-a716-446655440000")
        String uuid,

        @Schema(description = "루트 이름", example = "서울 고궁 투어")
        String title,

        @Schema(description = "공동 편집(공유) 여부", example = "false")
        boolean collaborative,

        @Schema(description = "경유지 목록")
        List<StopResponse> stops,

        @Schema(description = "이동 구간 목록")
        List<LegResponse> legs
) {}
