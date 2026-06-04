package com.eodigaljido.backend.dto.course;

import com.eodigaljido.backend.domain.chat.ChatRoom;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공동 루트 채팅방 응답")
public record CourseChatRoomResponse(
        @Schema(description = "루트 UUID")
        String courseUuid,

        @Schema(description = "연결된 채팅방 UUID")
        String chatRoomUuid,

        @Schema(description = "채팅방 이름")
        String roomName,

        @Schema(description = "채팅방 멤버 수")
        int memberCount
) {
    public static CourseChatRoomResponse of(String courseUuid, ChatRoom chatRoom, int memberCount) {
        return new CourseChatRoomResponse(
                courseUuid,
                chatRoom != null ? chatRoom.getUuid() : null,
                chatRoom != null ? chatRoom.getName() : null,
                memberCount
        );
    }
}
