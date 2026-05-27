package com.eodigaljido.backend.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "루트 채팅방 내 서브 루트 응답")
public record SubCourseResponse(
        @Schema(description = "서브 루트 채팅방 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        String chatRoomUuid,

        @Schema(description = "상위 루트 채팅방 UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String parentRoomUuid,

        @Schema(description = "연결된 루트 UUID", example = "7ecc5401-1234-5678-abcd-000000000001")
        String routeUuid,

        @Schema(description = "루트 이름", example = "마산버섯")
        String routeTitle,

        @Schema(description = "멤버 수", example = "3")
        int memberCount,

        @Schema(description = "마지막 메시지 내용 (없으면 null)", example = "마산 루트 수정했어요!")
        String lastMessage,

        @Schema(description = "마지막 메시지 전송 시각 (없으면 null)", example = "2026-05-27T10:00:00")
        LocalDateTime lastMessageAt,

        @Schema(description = "읽지 않은 메시지 수", example = "2")
        long unreadCount
) {}
