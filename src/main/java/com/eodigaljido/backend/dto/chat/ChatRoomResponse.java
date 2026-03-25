package com.eodigaljido.backend.dto.chat;

import java.time.LocalDateTime;

public record ChatRoomResponse(
        String uuid,
        String name,
        int memberCount,
        String lastMessage,
        LocalDateTime lastMessageAt,
        long unreadCount
) {}
