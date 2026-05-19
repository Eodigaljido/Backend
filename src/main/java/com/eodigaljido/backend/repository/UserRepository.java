package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    Optional<User> findByUuid(String uuid);
    Optional<User> findByUserId(String userId);
    Optional<User> findByFriendCode(String friendCode);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByUserId(String userId);
    boolean existsByFriendCode(String friendCode);

    @Query("SELECT u FROM User u WHERE u.friendCode IS NULL OR u.friendCode = ''")
    List<User> findUsersMissingFriendCode();
}
