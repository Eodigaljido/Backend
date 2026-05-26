package com.eodigaljido.backend.dto.user;

import com.eodigaljido.backend.domain.user.Profile;
import com.eodigaljido.backend.domain.user.User;
import com.eodigaljido.backend.domain.user.UserOAuthProvider;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record MyProfileResponse(
        String uuid,
        String friendCode,
        String email,
        String phone,
        List<String> loginMethods,
        String nickname,
        String profileImageUrl,
        boolean isDefaultImage,
        String bio,
        LocalDateTime createdAt,
        long sharedCourseCount,
        BigDecimal averageRating,
        long savedCourseCount
) {
    public static MyProfileResponse of(User user, Profile profile, List<UserOAuthProvider> oauthProviders,
                                       long sharedCourseCount, BigDecimal averageRating, long savedCourseCount) {
        List<String> methods = new ArrayList<>();
        if (user.isLocal()) methods.add("LOCAL");
        oauthProviders.forEach(op -> methods.add(op.getProvider().name()));

        return new MyProfileResponse(
                user.getUuid(),
                user.getFriendCode(),
                user.getEmail(),
                user.getPhone(),
                methods,
                profile != null ? profile.getNickname() : null,
                profile != null ? profile.getProfileImageUrl() : null,
                profile == null || profile.isDefaultImage(),
                profile != null ? profile.getBio() : null,
                user.getCreatedAt(),
                sharedCourseCount,
                averageRating,
                savedCourseCount
        );
    }
}
