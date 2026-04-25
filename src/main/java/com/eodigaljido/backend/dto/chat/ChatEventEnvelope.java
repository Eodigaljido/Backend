package com.eodigaljido.backend.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
        WebSocket 채팅 메시지 이벤트 (구독 경로: /topic/chat/{roomUuid})
        타이핑 이벤트는 별도 토픽(/topic/chat/{roomUuid}/typing)에서 TypingEvent 형식으로 수신합니다.
        """)
public record ChatEventEnvelope(
        @Schema(description = "이벤트 타입", example = "MESSAGE_CREATED",
                allowableValues = {"MESSAGE_CREATED", "MESSAGE_EDITED", "MESSAGE_DELETED"})
        EventType eventType,

        @Schema(description = "메시지 정보 (TEXT·IMAGE·ROUTE 모두 동일 구조, messageType으로 구분)")
        ChatMessageResponse payload
) {
    public enum EventType {
        /** 새 메시지 (TEXT / IMAGE / ROUTE) */
        MESSAGE_CREATED,
        /** 메시지 수정 (TEXT만 해당) */
        MESSAGE_EDITED,
        /** 메시지 삭제 */
        MESSAGE_DELETED
    }
}
