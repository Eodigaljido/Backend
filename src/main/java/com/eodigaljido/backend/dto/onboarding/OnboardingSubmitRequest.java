package com.eodigaljido.backend.dto.onboarding;

import jakarta.validation.constraints.NotBlank;

public record OnboardingSubmitRequest(
        @NotBlank(message = "region은 필수입니다.")
        String region,

        @NotBlank(message = "age는 필수입니다.")
        String age,

        @NotBlank(message = "activity는 필수입니다.")
        String activity,

        @NotBlank(message = "gender는 필수입니다.")
        String gender
) {}
