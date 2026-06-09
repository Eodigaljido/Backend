package com.eodigaljido.backend.domain.notification;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
        알림 유형:
        CHAT_MESSAGE(새 채팅 메시지), CHAT_MENTION(멘션), CHAT_ROOM_INVITED(채팅방 초대),
        CHAT_ROUTE_SHARED(채팅방 코스 공유), CHAT_ROUTE_CHANGED(채팅방 연결 코스 생성·수정),
        FRIEND_REQUESTED(친구 요청), FRIEND_ACCEPTED(친구 요청 수락),
        ROUTE_RECOMMENDED(맞춤 코스 추천), ROUTE_FAVORITED(코스 즐겨찾기), ROUTE_USED(코스 사용),
        GROUP_INVITED(모임 초대), GROUP_JOIN_REQUESTED(모임 가입 신청),
        GROUP_JOIN_APPROVED(모임 가입 승인), GROUP_JOIN_REJECTED(모임 가입 거절),
        GROUP_MEMBER_JOINED(새 모임 멤버), GROUP_POST_CREATED(새 모임 게시물),
        GROUP_ROUTE_CREATED(새 모임 루트), GROUP_CHAT_ROOM_CREATED(새 모임 채팅방)
        """)
public enum NotificationType {
    // 채팅
    CHAT_MESSAGE,
    CHAT_MENTION,
    CHAT_ROOM_INVITED,
    CHAT_ROUTE_SHARED,
    CHAT_ROUTE_CHANGED,

    // 친구
    FRIEND_REQUESTED,
    FRIEND_ACCEPTED,

    // 코스
    ROUTE_RECOMMENDED,
    ROUTE_FAVORITED,
    ROUTE_USED,

    // 모임
    GROUP_INVITED,
    GROUP_JOIN_REQUESTED,
    GROUP_JOIN_APPROVED,
    GROUP_JOIN_REJECTED,
    GROUP_MEMBER_JOINED,
    GROUP_POST_CREATED,
    GROUP_ROUTE_CREATED,
    GROUP_CHAT_ROOM_CREATED
}
