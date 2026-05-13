package com.eodigaljido.backend.dto.ai;

import lombok.Builder;

@Builder
public record MemberRoute(
        String memberName,
        String origin,
        String transportMode,
        int totalDurationMinutes,
        double totalDistanceKm
) {}
