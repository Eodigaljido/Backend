package com.eodigaljido.backend.dto.chat;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        String uuid,
        String senderUuid,
        String senderNickname,
        String senderProfileImageUrl,
        String content,
        LocalDateTime createdAt,
        LocalDateTime editedAt,
        boolean isDeleted
) {}
