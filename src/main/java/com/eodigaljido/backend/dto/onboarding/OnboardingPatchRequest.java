package com.eodigaljido.backend.dto.onboarding;

public record OnboardingPatchRequest(
        String region,
        String age,
        String activity,
        String gender
) {}
