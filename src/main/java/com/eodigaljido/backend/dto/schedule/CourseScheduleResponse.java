package com.eodigaljido.backend.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "코스 약속 응답")
public record CourseScheduleResponse(
        @Schema(description = "코스 약속 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        String uuid,

        @Schema(description = "약속 이름", example = "주말 카페 코스")
        String title,

        @Schema(description = "약속 일시. 서버는 UTC 기준으로 저장하고 응답합니다.", example = "2026-06-08T10:00:00Z")
        OffsetDateTime scheduledAt,

        @Schema(description = "연결된 채팅방 UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String chatRoomUuid,

        @Schema(description = "연결된 채팅방 이름", example = "주말 번개방")
        String chatRoomName,

        @Schema(description = "연결된 코스 UUID. 연결된 코스가 없으면 null입니다.", nullable = true, example = "7ecc5401-1234-5678-abcd-000000000001")
        String courseUuid,

        @Schema(description = "연결된 코스 제목. 연결된 코스가 없으면 null입니다.", nullable = true, example = "성수 카페 코스")
        String courseTitle,

        @Schema(description = "약속 생성자 UUID", example = "user-uuid-creator")
        String creatorUuid,

        @Schema(description = "약속 생성자 닉네임", example = "지민")
        String creatorNickname,

        @Schema(description = "약속 참여자 목록. 현재 채팅방 멤버 기준으로 채워집니다.")
        List<ParticipantSummary> participants,

        @Schema(description = "약속 생성 시각", example = "2026-06-02T03:00:00Z")
        OffsetDateTime createdAt,

        @Schema(description = "약속 수정 시각", example = "2026-06-02T03:00:00Z")
        OffsetDateTime updatedAt
) {}
