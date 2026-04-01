package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.onboarding.OnboardingAnswer;
import com.eodigaljido.backend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OnboardingAnswerRepository extends JpaRepository<OnboardingAnswer, Long> {

    Optional<OnboardingAnswer> findByUser(User user);

    boolean existsByUser(User user);
}
