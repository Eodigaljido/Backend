package com.eodigaljido.backend.dto.ai;

import lombok.Builder;

import java.util.List;

@Builder
public record IntentResult(
        List<String> destinations,
        String theme,
        Integer days,
        Integer budgetKrw,
        String travelType,
        List<String> searchKeywords,
        boolean nearby,
        List<String> freeKeywords,
        List<String> orderedPlaces
) {}
