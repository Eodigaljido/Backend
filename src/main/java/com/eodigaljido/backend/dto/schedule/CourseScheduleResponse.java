package com.eodigaljido.backend.dto.schedule;

import java.time.LocalDateTime;
import java.util.List;

public record CourseScheduleResponse(
    String id,
    String title,
    LocalDateTime scheduledAt,
    String chatRoomUuid,
    String chatRoomName,
    String memo,
    List<ParticipantSummary> participants,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
