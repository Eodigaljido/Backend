package com.eodigaljido.backend.dto.onboarding;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OnboardingStepAnswerBody(
        @NotEmpty(message = "answers는 필수입니다.")
        List<String> answers
) {}
