package com.eodigaljido.backend.dto.onboarding;

import java.util.List;

public record OnboardingPatchRequest(
        String region,
        String age,
        List<String> activity,
        String gender
) {}
