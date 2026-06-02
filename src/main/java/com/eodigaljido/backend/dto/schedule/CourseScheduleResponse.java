package com.eodigaljido.backend.dto.schedule;

import java.time.OffsetDateTime;
import java.util.List;

public record CourseScheduleResponse(
        String uuid,
        String title,
        OffsetDateTime scheduledAt,
        String chatRoomUuid,
        String chatRoomName,
        String courseUuid,
        String courseTitle,
        String creatorUuid,
        String creatorNickname,
        List<ParticipantSummary> participants,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
