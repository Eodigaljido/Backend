package com.eodigaljido.backend.domain.notification;

import java.util.Arrays;
import java.util.Optional;

public enum NotificationSettingKey {
    CHAT_MESSAGE("chatMessage"),
    CHAT_MENTION("chatMention"),
    CHAT_INVITE("chatInvite"),
    CHAT_COURSE_SHARED("chatCourseShared"),
    CHAT_COURSE_EDITED("chatCourseEdited"),
    FRIEND_REQUEST("friendRequest"),
    FRIEND_ACCEPTED("friendAccepted"),
    MEET_JOIN_REQUEST("meetJoinRequest"),
    MEET_JOIN_RESULT("meetJoinResult"),
    MEET_NEW_MEMBER("meetNewMember"),
    MEET_NEW_POST("meetNewPost"),
    MEET_NEW_ROUTE("meetNewRoute"),
    MEET_NEW_CHAT_ROOM("meetNewChatRoom"),
    COURSE_RECOMMENDED("courseRecommended"),
    COURSE_FAVORITED_OR_USED("courseFavoritedOrUsed");

    private final String key;

    NotificationSettingKey(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static Optional<NotificationSettingKey> fromKey(String key) {
        return Arrays.stream(values())
                .filter(settingKey -> settingKey.key.equals(key))
                .findFirst();
    }

    public static Optional<NotificationSettingKey> fromNotificationType(NotificationType type) {
        return Optional.ofNullable(switch (type) {
            case CHAT_MESSAGE -> CHAT_MESSAGE;
            case CHAT_MENTION -> CHAT_MENTION;
            case CHAT_ROOM_INVITED -> CHAT_INVITE;
            case CHAT_ROUTE_SHARED -> CHAT_COURSE_SHARED;
            case CHAT_ROUTE_CHANGED -> CHAT_COURSE_EDITED;
            case FRIEND_REQUESTED -> FRIEND_REQUEST;
            case FRIEND_ACCEPTED -> FRIEND_ACCEPTED;
            case GROUP_JOIN_REQUESTED -> MEET_JOIN_REQUEST;
            case GROUP_JOIN_APPROVED, GROUP_JOIN_REJECTED -> MEET_JOIN_RESULT;
            case GROUP_MEMBER_JOINED -> MEET_NEW_MEMBER;
            case GROUP_POST_CREATED -> MEET_NEW_POST;
            case GROUP_ROUTE_CREATED -> MEET_NEW_ROUTE;
            case GROUP_CHAT_ROOM_CREATED -> MEET_NEW_CHAT_ROOM;
            case ROUTE_RECOMMENDED -> COURSE_RECOMMENDED;
            case ROUTE_FAVORITED, ROUTE_USED -> COURSE_FAVORITED_OR_USED;
            case GROUP_INVITED -> null;
        });
    }
}
