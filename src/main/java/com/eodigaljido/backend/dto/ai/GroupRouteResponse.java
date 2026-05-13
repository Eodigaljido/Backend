package com.eodigaljido.backend.dto.ai;

import lombok.Builder;

import java.util.List;

@Builder
public record GroupRouteResponse(
        String message,
        List<String> buttons,
        List<MemberRoute> memberRoutes,
        int maxDurationMinutes,
        String sessionId
) {}
