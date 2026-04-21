package com.eodigaljido.backend.domain.notification;

import com.eodigaljido.backend.domain.common.BaseTimeEntity;
import com.eodigaljido.backend.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_notifications_recipient_read", columnList = "recipient_id, is_read"),
        @Index(name = "idx_notifications_recipient_created", columnList = "recipient_id, created_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 200)
    private String body;

    @Column(name = "reference_id", length = 36)
    private String referenceId;

    @Column(name = "reference_type", length = 20)
    private String referenceType;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean isRead = false;

    public void markAsRead() {
        this.isRead = true;
    }
}
