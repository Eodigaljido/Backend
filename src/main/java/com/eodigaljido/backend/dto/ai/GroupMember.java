package com.eodigaljido.backend.dto.ai;

import jakarta.validation.constraints.NotBlank;

public record GroupMember(
        @NotBlank String name,
        @NotBlank String origin,
        String transportMode,  // transit | driving | walking
        Integer estimatedDurationMinutes,
        Double estimatedDistanceKm
) {
    public GroupMember {
        if (transportMode == null) transportMode = "transit";
        if (estimatedDurationMinutes == null) estimatedDurationMinutes = 0;
        if (estimatedDistanceKm == null) estimatedDistanceKm = 0.0;
    }
}
