package com.eodigaljido.backend.dto.user;

import com.eodigaljido.backend.domain.user.Profile;
import com.eodigaljido.backend.domain.user.User;

public record UserSearchResponse(
        String uuid,
        String nickname,
        String profileImageUrl,
        boolean isDefaultImage
) {
    public static UserSearchResponse of(User user, Profile profile) {
        return new UserSearchResponse(
                user.getUuid(),
                profile != null ? profile.getNickname() : null,
                profile != null ? profile.getProfileImageUrl() : null,
                profile == null || profile.isDefaultImage()
        );
    }
}
