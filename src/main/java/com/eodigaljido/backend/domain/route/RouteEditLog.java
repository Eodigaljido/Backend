package com.eodigaljido.backend.domain.route;

import com.eodigaljido.backend.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "route_edit_logs",
    indexes = {
        @Index(name = "idx_route_edit_logs_route_id", columnList = "route_id"),
        @Index(name = "idx_route_edit_logs_editor_id", columnList = "editor_id"),
        @Index(name = "idx_route_edit_logs_created_at", columnList = "created_at")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class RouteEditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "editor_id", nullable = false)
    private User editor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EditAction action;

    @Column(name = "before_data", columnDefinition = "TEXT")
    private String beforeData;

    @Column(name = "after_data", columnDefinition = "TEXT")
    private String afterData;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum EditAction {
        ADD_WAYPOINT,
        UPDATE_WAYPOINT,
        REMOVE_WAYPOINT,
        ADD_LEG,
        UPDATE_LEG,
        REMOVE_LEG,
        UPDATE_TITLE,
        UPDATE_DESCRIPTION,
        UPDATE_THUMBNAIL,
        UPDATE_TAGS,
        UPDATE_REGION,
        UPDATE_ACTIVITY_TYPE
    }
}
