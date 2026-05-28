package com.eodigaljido.backend.dto.group;

import com.eodigaljido.backend.domain.chat.ChatRoom;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "모임 채팅방 응답")
public record GroupChatRoomResponse(
        @Schema(description = "채팅방 UUID") String uuid,
        @Schema(description = "채팅방 이름") String name,
        @Schema(description = "생성 일시") LocalDateTime createdAt
) {
    public static GroupChatRoomResponse from(ChatRoom room) {
        return new GroupChatRoomResponse(room.getUuid(), room.getName(), room.getCreatedAt());
    }
}
