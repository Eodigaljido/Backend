package com.eodigaljido.backend.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "타이핑 상태 전송 요청 (STOMP destination: /app/chat/{roomUuid}/typing)")
public record TypingRequest(
        @Schema(description = "타이핑 여부 (true: 타이핑 시작, false: 타이핑 중지)", example = "true")
        boolean isTyping
) {}
