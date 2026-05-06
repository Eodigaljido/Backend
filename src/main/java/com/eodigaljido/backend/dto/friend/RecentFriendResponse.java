package com.eodigaljido.backend.dto.friend;

import com.eodigaljido.backend.domain.friend.Friend;
import com.eodigaljido.backend.domain.user.Profile;
import com.eodigaljido.backend.domain.user.User;

import java.time.LocalDateTime;

public record RecentFriendResponse(
        Long friendId,
        String uuid,
        String nickname,
        String profileImageUrl,
        boolean isDefaultImage,
        LocalDateTime lastContactedAt
) {
    public static RecentFriendResponse of(Friend friend, User me, Profile otherProfile, LocalDateTime lastContactedAt) {
        User other = friend.getRequester().getId().equals(me.getId())
                ? friend.getReceiver()
                : friend.getRequester();
        return new RecentFriendResponse(
                friend.getId(),
                other.getUuid(),
                otherProfile != null ? otherProfile.getNickname() : null,
                otherProfile != null ? otherProfile.getProfileImageUrl() : null,
                otherProfile == null || otherProfile.isDefaultImage(),
                lastContactedAt
        );
    }
}
