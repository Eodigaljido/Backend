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
        @Index(name = "idx_users_phone", columnList = "phone")
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

    public boolean isLocal() {
        return this.passwordHash != null;
    }

    public void updateLastLoginAt(LocalDateTime at) {
        this.lastLoginAt = at;
    }

    public void updatePhone(String phone, LocalDateTime verifiedAt) {
        this.phone = phone;
        this.phoneVerifiedAt = verifiedAt;
    }

    public void markDeleted() {
        this.status = UserStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    public void linkLocalCredentials(String passwordHash, String phone, LocalDateTime verifiedAt) {
        this.passwordHash = passwordHash;
        this.phone = phone;
        this.phoneVerifiedAt = verifiedAt;
    }

    public enum Role {
        USER, ADMIN
    }

    public enum UserStatus {
        ACTIVE, SUSPENDED, DELETED
    }
}
