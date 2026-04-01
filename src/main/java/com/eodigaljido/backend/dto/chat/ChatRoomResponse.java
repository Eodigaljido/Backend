package com.eodigaljido.backend.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "채팅방 정보 응답")
public record ChatRoomResponse(
        @Schema(description = "채팅방 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        String uuid,

        @Schema(description = "채팅방 이름", example = "홍길동, 김철수")
        String name,

        @Schema(description = "현재 채팅방 멤버 수", example = "2")
        int memberCount,

        @Schema(description = "마지막 메시지 내용 (없으면 null)", example = "안녕하세요!")
        String lastMessage,

        @Schema(description = "마지막 메시지 전송 시각 (없으면 null)", example = "2026-04-01T10:00:00")
        LocalDateTime lastMessageAt,

        @Schema(description = "읽지 않은 메시지 수", example = "3")
        long unreadCount
) {}
