package com.eodigaljido.backend.dto.onboarding;

import com.eodigaljido.backend.domain.route.Route;

import java.util.List;

public record OnboardingSubmitResponse(
        String message,
        List<RecommendedRouteDto> recommendedRoutes
) {
    public record RecommendedRouteDto(
            String uuid,
            String title,
            String description,
            String thumbnailUrl,
            Integer estimatedTime
    ) {
        public static RecommendedRouteDto of(Route route) {
            return new RecommendedRouteDto(
                    route.getUuid(),
                    route.getTitle(),
                    route.getDescription(),
                    route.getThumbnailUrl(),
                    route.getEstimatedTime()
            );
        }
    }
}
