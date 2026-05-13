package com.eodigaljido.backend.dto.ai;

public record ContextSignalInput(
        boolean raining,
        boolean nighttime,
        boolean crowded,
        boolean rerouteRequested
) {}
