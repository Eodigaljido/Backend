package com.eodigaljido.backend.domain.notification;

public enum NotificationType {
    // 채팅
    CHAT_MESSAGE,       // 채팅방에 새 메시지
    CHAT_MENTION,       // 메시지에서 @멘션됨
    CHAT_ROOM_INVITED,  // 채팅방에 초대됨
    CHAT_ROUTE_SHARED,  // 채팅방에 코스 공유 (type=ROUTE 메시지)
    CHAT_ROUTE_CHANGED, // 채팅방 연결 코스 생성·수정

    // 코스
    ROUTE_RECOMMENDED,  // 취향 매칭 공유 코스 등록
    ROUTE_FAVORITED,    // 내 코스가 즐겨찾기됨
    ROUTE_USED          // 내 코스가 복사·사용됨
}
