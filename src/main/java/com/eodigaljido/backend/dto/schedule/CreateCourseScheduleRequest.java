package com.eodigaljido.backend.dto.schedule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record CreateCourseScheduleRequest(
        @NotBlank
        @Size(max = 100)
        String title,

        @NotNull
        OffsetDateTime scheduledAt,

        @NotNull
        String chatRoomUuid,

        String courseUuid,

        Boolean notifyChat
) {}
