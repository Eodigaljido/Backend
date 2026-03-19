package com.eodigaljido.backend.dto.user;

import com.eodigaljido.backend.domain.user.Profile;
import com.eodigaljido.backend.domain.user.User;

import java.time.LocalDateTime;

public record MyProfileResponse(
        String uuid,
        String email,
        String phone,
        String provider,
        String nickname,
        String profileImageUrl,
        boolean isDefaultImage,
        String bio,
        LocalDateTime createdAt
) {
    public static MyProfileResponse of(User user, Profile profile) {
        return new MyProfileResponse(
                user.getUuid(),
                user.getEmail(),
                user.getPhone(),
                user.getProvider().name(),
                profile != null ? profile.getNickname() : null,
                profile != null ? profile.getProfileImageUrl() : null,
                profile == null || profile.isDefaultImage(),
                profile != null ? profile.getBio() : null,
                user.getCreatedAt()
        );
    }
}
