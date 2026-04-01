package com.eodigaljido.backend.domain.friend;

import com.eodigaljido.backend.domain.common.BaseTimeEntity;
import com.eodigaljido.backend.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "friends",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_friends_requester_receiver", columnNames = {"requester_id", "receiver_id"})
    },
    indexes = {
        @Index(name = "idx_friends_requester_id", columnList = "requester_id"),
        @Index(name = "idx_friends_receiver_id", columnList = "receiver_id"),
        @Index(name = "idx_friends_status", columnList = "status")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Friend extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private FriendStatus status = FriendStatus.PENDING;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    public void accept() {
        this.status = FriendStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = FriendStatus.REJECTED;
        this.respondedAt = LocalDateTime.now();
    }

    public enum FriendStatus {
        PENDING, ACCEPTED, REJECTED, BLOCKED
    }
}
