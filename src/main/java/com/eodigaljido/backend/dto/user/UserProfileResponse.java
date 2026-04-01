package com.eodigaljido.backend.dto.user;

import com.eodigaljido.backend.domain.user.Profile;
import com.eodigaljido.backend.domain.user.User;

public record UserProfileResponse(
        String uuid,
        String nickname,
        String profileImageUrl,
        boolean isDefaultImage,
        String bio
) {
    public static UserProfileResponse of(User user, Profile profile) {
        return new UserProfileResponse(
                user.getUuid(),
                profile != null ? profile.getNickname() : null,
                profile != null ? profile.getProfileImageUrl() : null,
                profile == null || profile.isDefaultImage(),
                profile != null ? profile.getBio() : null
        );
    }
}
