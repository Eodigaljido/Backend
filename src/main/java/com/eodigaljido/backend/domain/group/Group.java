package com.eodigaljido.backend.domain.group;

import com.eodigaljido.backend.domain.common.BaseTimeEntity;
import com.eodigaljido.backend.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "groups",
    indexes = {
        @Index(name = "idx_groups_uuid", columnList = "uuid"),
        @Index(name = "idx_groups_created_by", columnList = "created_by"),
        @Index(name = "idx_groups_type", columnList = "type"),
        @Index(name = "idx_groups_status", columnList = "status")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Group extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 36, unique = true, nullable = false)
    private String uuid;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "profile_image_url", length = 512)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private GroupType type = GroupType.PUBLIC;

    @Column(name = "requires_approval", nullable = false)
    @Builder.Default
    private boolean requiresApproval = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private GroupStatus status = GroupStatus.ACTIVE;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private int viewCount = 0;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void update(String name, String description, String profileImageUrl,
                       GroupType type, boolean requiresApproval) {
        this.name = name;
        this.description = description;
        this.profileImageUrl = profileImageUrl;
        this.type = type;
        this.requiresApproval = requiresApproval;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void delete() {
        this.status = GroupStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    public enum GroupType {
        PUBLIC, PRIVATE
    }

    public enum GroupStatus {
        ACTIVE, DELETED
    }
}
