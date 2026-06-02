package com.eodigaljido.backend.dto.schedule;

import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record UpdateCourseScheduleRequest(
        @Size(max = 100)
        String title,

        OffsetDateTime scheduledAt,

        String chatRoomUuid,

        String courseUuid
) {}
