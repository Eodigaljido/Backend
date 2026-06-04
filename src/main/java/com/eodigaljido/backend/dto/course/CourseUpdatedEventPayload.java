package com.eodigaljido.backend.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "공동 루트 수정 이벤트 payload")
public record CourseUpdatedEventPayload(
        String courseUuid,
        long version,
        LocalDateTime updatedAt,
        String editorUuid,
        String editorNickname
) {}
