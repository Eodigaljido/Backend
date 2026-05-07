package com.eodigaljido.backend.domain.following;

import com.eodigaljido.backend.domain.common.BaseTimeEntity;
import com.eodigaljido.backend.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "following_news",
    indexes = {
        @Index(name = "idx_following_news_actor_created", columnList = "actor_id, created_at"),
        @Index(name = "idx_following_news_action_created", columnList = "action_type, created_at"),
        @Index(name = "idx_following_news_created", columnList = "created_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class FollowingNews extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private FollowingNewsActionType actionType;

    @Column(name = "course_id", length = 36)
    private String courseId;

    @Column(name = "course_name", length = 100)
    private String courseName;
}
