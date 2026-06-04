package com.eodigaljido.backend.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공동 루트 WebSocket 이벤트")
public record CourseEventEnvelope(
        @Schema(description = "이벤트 타입", example = "COURSE_UPDATED")
        EventType eventType,

        @Schema(description = "이벤트 payload")
        Object payload
) {
    public enum EventType {
        COURSE_UPDATED,
        COURSE_MEMBER_JOINED,
        COURSE_MEMBER_LEFT
    }
}
