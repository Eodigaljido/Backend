package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.user.Profile;
import com.eodigaljido.backend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
    Optional<Profile> findByUser(User user);
    boolean existsByNickname(String nickname);
    boolean existsByNicknameAndUserNot(String nickname, User user);
    List<Profile> findByNicknameContainingIgnoreCase(String keyword);
}
