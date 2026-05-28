package com.eodigaljido.backend.dto.schedule;

import java.time.LocalDateTime;

public record CourseScheduleNearestResponse(
    String id,
    String title,
    LocalDateTime scheduledAt,
    String chatRoomUuid,
    String chatRoomName,
    long participantsCount
) {}
