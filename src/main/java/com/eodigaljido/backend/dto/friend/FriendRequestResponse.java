package com.eodigaljido.backend.dto.friend;

import com.eodigaljido.backend.domain.friend.Friend;
import com.eodigaljido.backend.domain.user.Profile;

import java.time.LocalDateTime;

public record FriendRequestResponse(
        Long requestId,
        String uuid,
        String nickname,
        String profileImageUrl,
        boolean isDefaultImage,
        String direction,
        LocalDateTime createdAt
) {
    public static FriendRequestResponse ofSent(Friend friend, Profile receiverProfile) {
        return new FriendRequestResponse(
                friend.getId(),
                friend.getReceiver().getUuid(),
                receiverProfile != null ? receiverProfile.getNickname() : null,
                receiverProfile != null ? receiverProfile.getProfileImageUrl() : null,
                receiverProfile == null || receiverProfile.isDefaultImage(),
                "SENT",
                friend.getCreatedAt()
        );
    }

    public static FriendRequestResponse ofReceived(Friend friend, Profile requesterProfile) {
        return new FriendRequestResponse(
                friend.getId(),
                friend.getRequester().getUuid(),
                requesterProfile != null ? requesterProfile.getNickname() : null,
                requesterProfile != null ? requesterProfile.getProfileImageUrl() : null,
                requesterProfile == null || requesterProfile.isDefaultImage(),
                "RECEIVED",
                friend.getCreatedAt()
        );
    }
}
