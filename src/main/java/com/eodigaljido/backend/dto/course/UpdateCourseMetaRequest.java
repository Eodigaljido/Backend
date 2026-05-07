package com.eodigaljido.backend.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "코스 메타 정보 수정 요청 (미제공 필드는 기존 값 유지)")
public record UpdateCourseMetaRequest(
        @Schema(description = "코스 이름", example = "서울 고궁 투어 (수정)")
        String title,

        @Schema(description = "코스 설명", example = "수정된 고궁 탐방 루트 설명")
        String description,

        @Schema(description = "카테고리 (활동 유형)", example = "관광")
        String category,

        @Schema(description = "지역", example = "서울")
        String region
) {}
