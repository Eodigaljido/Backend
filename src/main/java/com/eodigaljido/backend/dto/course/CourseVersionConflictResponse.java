package com.eodigaljido.backend.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "공동 루트 버전 충돌 응답")
public record CourseVersionConflictResponse(
        int status,
        String message,
        long currentVersion,
        MyCourseDetailResponse course,
        LocalDateTime timestamp
) {}
