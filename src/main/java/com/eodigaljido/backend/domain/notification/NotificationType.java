package com.eodigaljido.backend.domain.notification;

public enum NotificationType {
    // 채팅
    CHAT_MESSAGE,       // 채팅방에 새 메시지
    CHAT_MENTION,       // 메시지에서 @멘션됨
    CHAT_ROOM_INVITED,  // 채팅방에 초대됨
    CHAT_ROUTE_SHARED,  // 채팅방에 코스 공유 (type=ROUTE 메시지)
    CHAT_ROUTE_CHANGED, // 채팅방 연결 코스 생성·수정

    // 코스
    ROUTE_RECOMMENDED,      // 취향 매칭 공유 코스 등록
    ROUTE_FAVORITED,        // 내 코스가 즐겨찾기됨
    ROUTE_USED,             // 내 코스가 복사·사용됨

    // 모임
    GROUP_INVITED,          // 모임 초대 알림 (초대받은 사람에게)
    GROUP_JOIN_REQUESTED,   // 모임 가입 요청 (관리자에게)
    GROUP_JOIN_APPROVED,    // 모임 가입 승인됨 (요청자에게)
    GROUP_JOIN_REJECTED,    // 모임 가입 거절됨 (요청자에게)
    GROUP_POST_CREATED,     // 모임에 게시물 등록됨 (멤버들에게)
    GROUP_ROUTE_CREATED     // 모임에 루트 등록됨 (멤버들에게)
}
