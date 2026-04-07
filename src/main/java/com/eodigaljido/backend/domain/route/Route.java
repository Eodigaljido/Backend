package com.eodigaljido.backend.domain.route;

import com.eodigaljido.backend.domain.common.BaseTimeEntity;
import com.eodigaljido.backend.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "routes",
    indexes = {
        @Index(name = "idx_routes_uuid", columnList = "uuid"),
        @Index(name = "idx_routes_user_id", columnList = "user_id"),
        @Index(name = "idx_routes_status", columnList = "status"),
        @Index(name = "idx_routes_is_shared", columnList = "is_shared")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Route extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 36, unique = true, nullable = false)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 100, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RouteStatus status = RouteStatus.DRAFT;

    @Column(name = "is_shared", nullable = false)
    @Builder.Default
    private boolean isShared = false;

    @Column(name = "total_distance", precision = 10, scale = 2)
    private BigDecimal totalDistance;

    @Column(name = "estimated_time")
    private Integer estimatedTime;

    @Column(name = "thumbnail_url", length = 512)
    private String thumbnailUrl;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public enum RouteStatus {
        DRAFT, PUBLISHED, DELETED
    }

    public void update(String title, String description, BigDecimal totalDistance,
                       Integer estimatedTime, String thumbnailUrl) {
        this.title = title;
        this.description = description;
        this.totalDistance = totalDistance;
        this.estimatedTime = estimatedTime;
        this.thumbnailUrl = thumbnailUrl;
    }

    public void updateStatus(RouteStatus status) {
        this.status = status;
    }

    public void enableSharing() {
        this.isShared = true;
    }

    public void disableSharing() {
        this.isShared = false;
    }

    public void markDeleted() {
        this.status = RouteStatus.DELETED;
        this.isShared = false;
        this.deletedAt = LocalDateTime.now();
    }
}
