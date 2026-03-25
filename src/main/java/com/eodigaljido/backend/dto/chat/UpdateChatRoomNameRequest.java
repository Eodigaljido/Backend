package com.eodigaljido.backend.dto.chat;

import jakarta.validation.constraints.Size;

public record UpdateChatRoomNameRequest(
        @Size(max = 100, message = "채팅방 이름은 100자를 초과할 수 없습니다.") String name
) {}
