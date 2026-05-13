package com.eodigaljido.backend.dto.ai;

public record RouteWaypointInput(
        Integer sequence,
        String name,
        double latitude,
        double longitude,
        String address,
        String memo
) {}
