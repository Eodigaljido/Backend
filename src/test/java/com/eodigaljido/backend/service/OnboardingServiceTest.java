package com.eodigaljido.backend.service;

import com.eodigaljido.backend.domain.onboarding.OnboardingAnswer;
import com.eodigaljido.backend.domain.user.User;
import com.eodigaljido.backend.dto.onboarding.OnboardingAnswersResponse;
import com.eodigaljido.backend.repository.OnboardingAnswerRepository;
import com.eodigaljido.backend.repository.RouteRepository;
import com.eodigaljido.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OnboardingServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final OnboardingAnswerRepository onboardingAnswerRepository = mock(OnboardingAnswerRepository.class);
    private final OnboardingService service = new OnboardingService(
            userRepository,
            onboardingAnswerRepository,
            mock(RouteRepository.class)
    );

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).uuid("user-uuid").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    }

    @Test
    void returnsSavedAnswersForRecommendationSorting() {
        OnboardingAnswer answer = OnboardingAnswer.builder()
                .user(user)
                .region("Seoul")
                .age("20s")
                .activity(List.of("walking", "culture"))
                .gender("female")
                .build();
        answer.complete();
        when(onboardingAnswerRepository.findByUser(user)).thenReturn(Optional.of(answer));

        OnboardingAnswersResponse response = service.getAnswers(1L);

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.currentStep()).isEqualTo(4);
        assertThat(response.region()).isEqualTo("Seoul");
        assertThat(response.age()).isEqualTo("20s");
        assertThat(response.activity()).containsExactly("walking", "culture");
        assertThat(response.gender()).isEqualTo("female");
        assertThat(response.completedAt()).isNotNull();
    }
}
