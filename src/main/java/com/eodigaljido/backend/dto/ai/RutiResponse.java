package com.eodigaljido.backend.dto.ai;

import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record RutiResponse(
        String message,
        List<String> buttons,
        String action,
        Map<String, Object> payload
) {}
