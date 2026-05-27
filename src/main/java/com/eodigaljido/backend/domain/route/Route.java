package com.eodigaljido.backend.domain.route;

import com.eodigaljido.backend.domain.chat.ChatRoom;
import com.eodigaljido.backend.domain.common.BaseTimeEntity;
import com.eodigaljido.backend.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "is_collaborative", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean isCollaborative = false;

    @Column(name = "total_distance", precision = 10, scale = 2)
    private BigDecimal totalDistance;

    @Column(name = "estimated_time")
    private Integer estimatedTime;

    @Column(name = "thumbnail_url", length = 512)
    private String thumbnailUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_room_id")
    private ChatRoom teamRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @Column(name = "region", length = 50)
    private String region;

    @Column(name = "activity_type", length = 50)
    private String activityType;

    @Column(name = "views", nullable = false)
    @Builder.Default
    private int views = 0;

    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private int reviewCount = 0;

    @Column(name = "average_rating", precision = 3, scale = 2)
    private java.math.BigDecimal averageRating;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "route_tags", joinColumns = @JoinColumn(name = "route_id"))
    @Column(name = "tag", length = 20)
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    public enum RouteStatus {
        DRAFT, PUBLISHED, DELETED
    }

    public void incrementViews() {
        this.views++;
    }

    public void updateRatingStats(java.math.BigDecimal averageRating, int reviewCount) {
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
    }

    public void update(String title, String description, BigDecimal totalDistance,
                       Integer estimatedTime, String thumbnailUrl, String region, String activityType) {
        this.title = title;
        this.description = description;
        this.totalDistance = totalDistance;
        this.estimatedTime = estimatedTime;
        this.thumbnailUrl = thumbnailUrl;
        this.region = region;
        this.activityType = activityType;
    }

    public void updateThumbnailUrl(String thumbnailUrl) {
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

    public void enableCollaboration() {
        this.isCollaborative = true;
    }

    public void disableCollaboration() {
        this.isCollaborative = false;
    }

    public void assignTeamRoom(ChatRoom teamRoom) {
        this.teamRoom = teamRoom;
    }

    public void assignChatRoom(ChatRoom chatRoom) {
        this.chatRoom = chatRoom;
    }

    public void markDeleted() {
        this.status = RouteStatus.DELETED;
        this.isShared = false;
        this.isCollaborative = false;
        this.deletedAt = LocalDateTime.now();
    }

    public void updateTags(List<String> newTags) {
        this.tags.clear();
        if (newTags != null) {
            this.tags.addAll(newTags);
        }
    }
}
