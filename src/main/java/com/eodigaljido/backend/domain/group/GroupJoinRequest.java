package com.eodigaljido.backend.domain.group;

import com.eodigaljido.backend.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "group_join_requests",
    indexes = {
        @Index(name = "idx_group_join_req_group_id", columnList = "group_id"),
        @Index(name = "idx_group_join_req_requester_id", columnList = "requester_id"),
        @Index(name = "idx_group_join_req_status", columnList = "status")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class GroupJoinRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

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
