package com.eodigaljido.backend.dto.onboarding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OnboardingSubmitRequest(
        @NotBlank(message = "region은 필수입니다.")
        String region,

        @NotBlank(message = "age는 필수입니다.")
        String age,

        @NotEmpty(message = "activity는 필수입니다.")
        List<String> activity,

        @NotBlank(message = "gender는 필수입니다.")
        String gender
) {}
