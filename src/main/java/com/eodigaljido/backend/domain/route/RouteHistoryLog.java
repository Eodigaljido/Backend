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
    private Type type;

    // CHAT 액션의 메시지 내용 스냅샷 (전송 당시/수정 후/삭제 직전 내용)
    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "edit_action", length = 32)
    private String editAction;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum Type {
        CHAT, COURSE,
        /**
         * Legacy value kept so Hibernate does not destructively narrow the production enum.
         * New records must use CHAT or COURSE.
         */
        EDIT
    }

    public Type getEffectiveType() {
        if (type != Type.EDIT) {
            return type;
        }
        return editAction != null && editAction.startsWith("CHAT_")
                ? Type.CHAT
                : Type.COURSE;
    }

    public String getEffectiveEditAction() {
        if (editAction != null && !editAction.isBlank()) {
            return editAction;
        }
        return getEffectiveType() == Type.CHAT ? "CHAT_SENDED" : "ROUTE_UPDATED";
    }
}
