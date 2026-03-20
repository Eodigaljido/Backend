package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    Optional<User> findByUuid(String uuid);
    Optional<User> findByProviderAndProviderId(User.Provider provider, String providerId);
    boolean existsByProviderAndProviderId(User.Provider provider, String providerId);
    Optional<User> findByLinkedKakaoId(String linkedKakaoId);
    boolean existsByLinkedKakaoId(String linkedKakaoId);
    Optional<User> findByLinkedGoogleId(String linkedGoogleId);
    boolean existsByLinkedGoogleId(String linkedGoogleId);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
}
