package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.onboarding.OnboardingAnswer;
import com.eodigaljido.backend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OnboardingAnswerRepository extends JpaRepository<OnboardingAnswer, Long> {

    Optional<OnboardingAnswer> findByUser(User user);

    boolean existsByUser(User user);

    @Query("SELECT a FROM OnboardingAnswer a WHERE " +
           "(:region IS NULL OR a.region = :region) OR " +
           "(:activityType IS NULL OR a.activity = :activityType)")
    List<OnboardingAnswer> findMatchingUsers(@Param("region") String region,
                                             @Param("activityType") String activityType);
}
