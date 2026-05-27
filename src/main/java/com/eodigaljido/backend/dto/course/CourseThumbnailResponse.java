package com.eodigaljido.backend.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "코스 대표 이미지 응답")
public record CourseThumbnailResponse(
        @Schema(description = "대표 이미지 URL. 삭제 후에는 null", example = "https://cdn.example.com/courses/550e8400/cover.jpg")
        String thumbnail
) {}
