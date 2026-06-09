package com.eodigaljido.backend.domain.notification;

import com.eodigaljido.backend.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "notification_settings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_settings_user_key",
                columnNames = {"user_id", "setting_key"}
        ),
        indexes = @Index(name = "idx_notification_settings_user_id", columnList = "user_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class NotificationSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "setting_key", nullable = false, length = 40)
    private NotificationSettingKey settingKey;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    public void updateEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
