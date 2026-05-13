package com.eodigaljido.backend.dto.ai;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record AiChatRequest(
        @NotBlank String message,
        String sessionId,
        String channel, // chat | voice
        String phase,   // pre_trip | in_trip
        RouteSnapshotInput routeSnapshot,
        ContextSignalInput contextSignals,
        Double lat,
        Double lng,
        Long routeId,
        Long userId,
        List<RouteWaypointInput> waypoints
) {
    public AiChatRequest {
        if (channel == null || channel.isBlank()) channel = "chat";
        if (phase == null || phase.isBlank()) phase = "pre_trip";
        if (waypoints == null) waypoints = List.of();
    }
}
