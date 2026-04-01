package com.eodigaljido.backend.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "WebSocket 채팅 이벤트 응답 (구독 경로: /topic/chat/{roomUuid})")
public record ChatEventEnvelope(
        @Schema(description = "이벤트 타입", example = "MESSAGE_CREATED")
        EventType eventType,

        @Schema(description = "메시지 정보")
        ChatMessageResponse payload
) {
    public enum EventType {
        /** 새 메시지 전송 */
        MESSAGE_CREATED,
        /** 메시지 수정 */
        MESSAGE_EDITED,
        /** 메시지 삭제 */
        MESSAGE_DELETED
    }
}
