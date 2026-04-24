package com.eodigaljido.backend.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "채팅방 정보 응답")
public record ChatRoomResponse(
        @Schema(description = "채팅방 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        String uuid,

        @Schema(description = "채팅방 이름", example = "김철수의 채팅방")
        String name,

        @Schema(description = "프로필 이미지 URL (그룹 채팅방은 채팅방 이미지, 1:1 채팅방은 상대방 프로필 이미지, 없으면 null)", example = "/images/chat/group-default-1.png")
        String profileImageUrl,

        @Schema(description = "현재 채팅방 멤버 수", example = "2")
        int memberCount,

        @Schema(description = "방장 UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String ownerUuid,

        @Schema(description = "방장 아이디", example = "john123")
        String ownerUserId,

        @Schema(description = "입장 순서 기준 최대 3명의 멤버 UUID 목록 (전체 인원은 memberCount 참고)",
                example = "[\"a1b2c3d4-e5f6-7890-abcd-ef1234567890\", \"550e8400-e29b-41d4-a716-446655440001\", \"550e8400-e29b-41d4-a716-446655440002\"]")
        List<String> memberUuids,

        @Schema(description = "입장 순서 기준 최대 3명의 멤버 아이디 목록 (전체 인원은 memberCount 참고)",
                example = "[\"john123\", \"jane456\", \"kim789\"]")
        List<String> memberUserIds,

        @Schema(description = "마지막 메시지 내용 (없으면 null)", example = "안녕하세요!")
        String lastMessage,

        @Schema(description = "마지막 메시지 전송 시각 (없으면 null)", example = "2026-04-01T10:00:00")
        LocalDateTime lastMessageAt,

        @Schema(description = "읽지 않은 메시지 수", example = "3")
        long unreadCount
) {}
