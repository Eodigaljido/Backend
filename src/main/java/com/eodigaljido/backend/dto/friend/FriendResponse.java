package com.eodigaljido.backend.dto.friend;

import com.eodigaljido.backend.domain.friend.Friend;
import com.eodigaljido.backend.domain.user.Profile;
import com.eodigaljido.backend.domain.user.User;

public record FriendResponse(
        Long friendId,
        String uuid,
        String nickname,
        String profileImageUrl,
        boolean isDefaultImage
) {
    public static FriendResponse of(Friend friend, User me, Profile otherProfile) {
        User other = friend.getRequester().getId().equals(me.getId())
                ? friend.getReceiver()
                : friend.getRequester();
        return new FriendResponse(
                friend.getId(),
                other.getUuid(),
                otherProfile != null ? otherProfile.getNickname() : null,
                otherProfile != null ? otherProfile.getProfileImageUrl() : null,
                otherProfile == null || otherProfile.isDefaultImage()
        );
    }
}
