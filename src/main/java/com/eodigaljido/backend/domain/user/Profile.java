package com.eodigaljido.backend.domain.user;

import com.eodigaljido.backend.domain.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "profiles",
    indexes = {
        @Index(name = "idx_profiles_nickname", columnList = "nickname")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class Profile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 50, unique = true, nullable = false)
    private String nickname;

    @Column(name = "profile_image_url", length = 512)
    private String profileImageUrl;

    @Column(name = "is_default_image", nullable = false)
    @Builder.Default
    private boolean isDefaultImage = true;

    @Column(length = 255)
    private String bio;

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateBio(String bio) {
        this.bio = bio;
    }

    public void updateProfileImage(String imageUrl) {
        this.profileImageUrl = imageUrl;
        this.isDefaultImage = false;
    }

    public void resetToDefaultImage() {
        this.profileImageUrl = null;
        this.isDefaultImage = true;
    }
}
