package com.eodigaljido.backend.domain.route;

import com.eodigaljido.backend.domain.common.BaseTimeEntity;
import com.eodigaljido.backend.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "course_members",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_course_members_route_user", columnNames = {"route_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_course_members_route_id", columnList = "route_id"),
                @Index(name = "idx_course_members_user_id", columnList = "user_id"),
                @Index(name = "idx_course_members_role", columnList = "role")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class CourseMember extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private Role role = Role.EDITOR;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    public void updateRole(Role role) {
        this.role = role;
    }

    public void rejoin(Role role) {
        this.leftAt = null;
        this.role = role;
    }

    public void leave() {
        this.leftAt = LocalDateTime.now();
    }

    public void touchLastSeen() {
        this.lastSeenAt = LocalDateTime.now();
    }

    public boolean canEdit() {
        return role == Role.OWNER || role == Role.EDITOR;
    }

    public enum Role {
        OWNER, EDITOR, VIEWER
    }
}
