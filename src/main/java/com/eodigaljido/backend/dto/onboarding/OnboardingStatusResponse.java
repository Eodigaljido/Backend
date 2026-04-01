package com.eodigaljido.backend.dto.onboarding;

import com.eodigaljido.backend.domain.onboarding.OnboardingAnswer;

public record OnboardingStatusResponse(
        String status,
        boolean completed,
        int currentStep
) {
    public static OnboardingStatusResponse notStarted() {
        return new OnboardingStatusResponse("NOT_STARTED", false, 0);
    }

    public static OnboardingStatusResponse of(OnboardingAnswer answer) {
        boolean completed = answer.getStatus() == OnboardingAnswer.OnboardingStatus.COMPLETED;
        String status = answer.getStatus().name();
        int currentStep = completed ? 4 : answer.getCurrentStep();
        return new OnboardingStatusResponse(status, completed, currentStep);
    }
}
