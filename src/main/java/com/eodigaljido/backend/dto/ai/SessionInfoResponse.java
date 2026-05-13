package com.eodigaljido.backend.dto.ai;

public record SessionInfoResponse(
        String sessionId,
        int turnCount,
        int messageCount,
        Long lastActive
) {}
