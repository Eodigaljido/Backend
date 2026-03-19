package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.user.RefreshToken;
import com.eodigaljido.backend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHashAndRevokedAtIsNull(String tokenHash);
    List<RefreshToken> findAllByUserAndRevokedAtIsNull(User user);
}
