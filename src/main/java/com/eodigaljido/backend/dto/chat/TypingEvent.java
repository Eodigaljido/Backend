package com.eodigaljido.backend.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "타이핑 이벤트 (구독 경로: /topic/chat/{roomUuid}/typing)")
public record TypingEvent(
        @Schema(description = "타이핑 중인 유저의 UUID", example = "550e8400-e29b-41d4-a716-446655440002")
        String senderUuid,

        @Schema(description = "타이핑 중인 유저의 닉네임", example = "홍길동")
        String senderNickname,

        @Schema(description = "타이핑 여부 (true: 타이핑 중, false: 타이핑 중지)", example = "true")
        boolean isTyping
) {}
