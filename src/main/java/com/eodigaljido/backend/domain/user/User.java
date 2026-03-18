package com.eodigaljido.backend.domain.user;

import com.eodigaljido.backend.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_users_uuid", columnList = "uuid"),
        @Index(name = "idx_users_email", columnList = "email"),
        @Index(name = "idx_users_phone", columnList = "phone"),
        @Index(name = "idx_users_provider", columnList = "provider, provider_id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 36, unique = true, nullable = false)
    private String uuid;

    @Column(length = 255, unique = true)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(length = 20, unique = true)
    private String phone;

    @Column(name = "phone_verified_at")
    private LocalDateTime phoneVerifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Provider provider;

    @Column(name = "provider_id", length = 255)
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public enum Provider {
        LOCAL, GOOGLE, KAKAO
    }

    public enum Role {
        USER, ADMIN
    }

    public enum UserStatus {
        ACTIVE, SUSPENDED, DELETED
    }
}
