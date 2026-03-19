package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    Optional<User> findByProviderAndProviderId(User.Provider provider, String providerId);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
}
