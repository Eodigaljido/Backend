package com.eodigaljido.backend.dto.chat;

public record ChatEventEnvelope(
        EventType eventType,
        ChatMessageResponse payload
) {
    public enum EventType {
        MESSAGE_CREATED,
        MESSAGE_EDITED,
        MESSAGE_DELETED
    }
}
