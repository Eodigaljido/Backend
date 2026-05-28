package com.eodigaljido.backend.dto.schedule;

import java.util.List;

public record CourseScheduleListResponse(
    List<CourseScheduleItem> items,
    int page,
    int size,
    long total
) {
    public record CourseScheduleItem(
        String id,
        String title,
        java.time.LocalDateTime scheduledAt,
        String chatRoomUuid,
        String chatRoomName,
        List<ParticipantSummary> participants
    ) {}
}
