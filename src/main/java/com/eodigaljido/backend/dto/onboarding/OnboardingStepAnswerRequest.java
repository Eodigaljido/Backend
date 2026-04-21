package com.eodigaljido.backend.dto.onboarding;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OnboardingStepAnswerRequest(
        @NotNull(message = "step은 필수입니다.")
        @Min(value = 1, message = "step은 1 이상이어야 합니다.")
        @Max(value = 4, message = "step은 4 이하여야 합니다.")
        Integer step,

        @NotEmpty(message = "answers는 필수입니다.")
        List<String> answers
) {}
