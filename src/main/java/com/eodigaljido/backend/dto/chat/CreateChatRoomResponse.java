package com.eodigaljido.backend.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "채팅방 생성 응답")
public record CreateChatRoomResponse(
        @Schema(description = "채팅방 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        String uuid,

        @Schema(description = "채팅방 이름", example = "김철수의 채팅방")
        String name,

        @Schema(description = "현재 채팅방 멤버 수", example = "2")
        int memberCount,

        @Schema(description = "방장 UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String ownerUuid,

        @Schema(description = "방장 아이디", example = "john123")
        String ownerUserId,

        @Schema(description = "멤버 UUID 목록 (방장 포함)", example = "[\"a1b2c3d4-...\", \"550e8400-...\"]")
        List<String> memberUuids,

        @Schema(description = "멤버 아이디 목록 (방장 포함)", example = "[\"john123\", \"jane456\"]")
        List<String> memberUserIds
) {}
