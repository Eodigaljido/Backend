package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.user.PhoneVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, Long> {
    Optional<PhoneVerification> findTopByPhoneAndPurposeAndVerifiedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
            String phone, PhoneVerification.Purpose purpose, LocalDateTime now);

    Optional<PhoneVerification> findTopByPhoneAndPurposeAndVerifiedTrueAndExpiresAtAfterOrderByCreatedAtDesc(
            String phone, PhoneVerification.Purpose purpose, LocalDateTime now);
}
