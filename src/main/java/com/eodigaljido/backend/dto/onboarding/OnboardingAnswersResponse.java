package com.eodigaljido.backend.dto.onboarding;

import com.eodigaljido.backend.domain.onboarding.OnboardingAnswer;

import java.util.List;

public record OnboardingAnswersResponse(
        String status,
        int currentStep,
        String region,
        String age,
        List<String> activity,
        String gender
) {
    public static OnboardingAnswersResponse of(OnboardingAnswer answer) {
        return new OnboardingAnswersResponse(
                answer.getStatus().name(),
                answer.getCurrentStep(),
                answer.getRegion(),
                answer.getAge(),
                answer.getActivity(),
                answer.getGender()
        );
    }
}
