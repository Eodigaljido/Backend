package com.eodigaljido.backend.domain.user;

import com.eodigaljido.backend.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "user_oauth_providers",
    uniqueConstraints = @UniqueConstraint(name = "uk_uop_provider_id", columnNames = {"provider", "provider_id"}),
    indexes = {
        @Index(name = "idx_uop_user_id", columnList = "user_id"),
        @Index(name = "idx_uop_provider_pid", columnList = "provider, provider_id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class UserOAuthProvider extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OAuthProvider provider;

    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    public enum OAuthProvider {
        GOOGLE, KAKAO
    }

    public static UserOAuthProvider of(User user, OAuthProvider provider, String providerId) {
        return UserOAuthProvider.builder()
                .user(user)
                .provider(provider)
                .providerId(providerId)
                .build();
    }
}
