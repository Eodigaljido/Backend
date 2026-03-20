package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.user.User;
import com.eodigaljido.backend.domain.user.UserOAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserOAuthProviderRepository extends JpaRepository<UserOAuthProvider, Long> {
    Optional<UserOAuthProvider> findByProviderAndProviderId(UserOAuthProvider.OAuthProvider provider, String providerId);
    boolean existsByProviderAndProviderId(UserOAuthProvider.OAuthProvider provider, String providerId);
    Optional<UserOAuthProvider> findByUserAndProvider(User user, UserOAuthProvider.OAuthProvider provider);
    boolean existsByUserAndProvider(User user, UserOAuthProvider.OAuthProvider provider);
    List<UserOAuthProvider> findAllByUser(User user);
    long countByUser(User user);
    void deleteByUserAndProvider(User user, UserOAuthProvider.OAuthProvider provider);
}
