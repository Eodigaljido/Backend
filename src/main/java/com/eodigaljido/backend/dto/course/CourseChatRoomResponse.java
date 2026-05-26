package com.eodigaljido.backend.dto.course;

import com.eodigaljido.backend.domain.chat.ChatRoom;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공동 루트 채팅방 UUID 조회 응답")
public record CourseChatRoomResponse(
        @Schema(description = "루트 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        String courseUuid,

        @Schema(description = "연결된 채팅방 UUID (공동 루트가 아니거나 채팅방 미생성 시 null)")
        String chatRoomUuid
) {
    public static CourseChatRoomResponse of(String courseUuid, ChatRoom chatRoom) {
        return new CourseChatRoomResponse(
                courseUuid,
                chatRoom != null ? chatRoom.getUuid() : null
        );
    }
}
