package com.eodigaljido.backend.domain.group;

import com.eodigaljido.backend.domain.common.BaseTimeEntity;
import com.eodigaljido.backend.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "group_posts",
    indexes = {
        @Index(name = "idx_group_posts_uuid", columnList = "uuid"),
        @Index(name = "idx_group_posts_group_id", columnList = "group_id"),
        @Index(name = "idx_group_posts_author_id", columnList = "author_id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class GroupPost extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 36, unique = true, nullable = false)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "group_post_images", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "image_url", length = 512)
    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void update(String content, List<String> imageUrls) {
        this.content = content;
        this.imageUrls.clear();
        if (imageUrls != null) {
            this.imageUrls.addAll(imageUrls);
        }
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
