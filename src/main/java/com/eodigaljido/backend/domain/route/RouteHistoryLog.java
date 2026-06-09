package com.eodigaljido.backend.domain.route;

import com.eodigaljido.backend.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "route_history_logs",
    indexes = {
        @Index(name = "idx_route_history_logs_route_id", columnList = "route_id"),
        @Index(name = "idx_route_history_logs_created_at", columnList = "created_at")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class RouteHistoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LogType type;

    // CHAT 타입: 전송 시점의 메시지 내용 스냅샷 (이후 수정/삭제와 무관)
    @Column(columnDefinition = "TEXT")
    private String content;

    // EDIT 타입: 어떤 편집이 발생했는지 (예: ADD_WAYPOINT, ROUTE_UPDATED)
    @Column(name = "edit_action", length = 30)
    private String editAction;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum LogType {
        CHAT, EDIT
    }
}
