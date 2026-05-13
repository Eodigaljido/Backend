package com.eodigaljido.backend.dto.ai;

import java.util.List;

public record RouteSnapshotInput(
        String destination,
        String transportMode,
        Integer remainingMinutes,
        List<RouteWaypointInput> waypoints
) {
    public RouteSnapshotInput {
        if (transportMode == null || transportMode.isBlank()) transportMode = "walking";
        if (waypoints == null) waypoints = List.of();
    }
}
