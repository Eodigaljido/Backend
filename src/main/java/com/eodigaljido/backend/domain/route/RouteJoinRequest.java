package com.eodigaljido.backend.domain.route;

import com.eodigaljido.backend.domain.chat.ChatRoom;
import com.eodigaljido.backend.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "route_join_requests",
    indexes = {
        @Index(name = "idx_route_join_req_room", columnList = "chat_room_id"),
        @Index(name = "idx_route_join_req_requester", columnList = "requester_id"),
        @Index(name = "idx_route_join_req_status", columnList = "status")
    },
    uniqueConstraints = @UniqueConstraint(
        name = "uq_route_join_req_room_requester",
        columnNames = {"chat_room_id", "requester_id"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class RouteJoinRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy;

    public void approve(User admin) {
        this.status = RequestStatus.APPROVED;
        this.processedAt = LocalDateTime.now();
        this.processedBy = admin;
    }

    public void reject(User admin) {
        this.status = RequestStatus.REJECTED;
        this.processedAt = LocalDateTime.now();
        this.processedBy = admin;
    }

    public enum RequestStatus {
        PENDING, APPROVED, REJECTED
    }
}
