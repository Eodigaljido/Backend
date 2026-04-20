package com.eodigaljido.backend.dto.onboarding;

import jakarta.validation.constraints.NotBlank;

public record OnboardingStepAnswerBody(
        @NotBlank(message = "answer는 필수입니다.")
        String answer
) {}
