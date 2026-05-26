package com.eodigaljido.backend.dto.user;

import com.eodigaljido.backend.domain.user.Profile;
import com.eodigaljido.backend.domain.user.User;

import java.math.BigDecimal;

public record UserProfileResponse(
        String uuid,
        String email,
        String nickname,
        String profileImageUrl,
        boolean isDefaultImage,
        String bio,
        long sharedCourseCount,
        BigDecimal averageRating,
        long savedCourseCount
) {
    public static UserProfileResponse of(User user, Profile profile,
                                         long sharedCourseCount, BigDecimal averageRating, long savedCourseCount) {
        return new UserProfileResponse(
                user.getUuid(),
                user.getEmail(),
                profile != null ? profile.getNickname() : null,
                profile != null ? profile.getProfileImageUrl() : null,
                profile == null || profile.isDefaultImage(),
                profile != null ? profile.getBio() : null,
                sharedCourseCount,
                averageRating,
                savedCourseCount
        );
    }
}
