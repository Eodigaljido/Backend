package com.eodigaljido.backend.dto.routehistory;

import com.eodigaljido.backend.domain.chat.ChatMessage;
import com.eodigaljido.backend.domain.user.Profile;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "루트 기록 상세 항목 (채팅 메시지 또는 루트 수정 이벤트)")
public record RouteHistoryFeedItem(

        @Schema(
            description = "이벤트 타입. CHAT: 채팅 메시지 관련 이벤트, COURSE: 루트 수정 이벤트",
            example = "CHAT",
            allowableValues = {"CHAT", "COURSE"}
        )
        String type,

        @Schema(description = "이벤트 고유 ID (chat_messages PK)", example = "42")
        Long itemId,

        @Schema(description = "이벤트를 발생시킨 사용자 UUID", example = "770e8400-e29b-41d4-a716-446655440002")
        String actorUuid,

        @Schema(description = "이벤트를 발생시킨 사용자 닉네임", example = "홍길동")
        String actorNickname,

        @Schema(description = "이벤트를 발생시킨 사용자 프로필 이미지 URL (없으면 null)", example = "https://example.com/profile.jpg")
        String actorProfileImageUrl,

        @Schema(
            description = "메시지 내용. type=CHAT이면 항상 존재 (전송/수정 후/삭제 직전 내용의 스냅샷). type=COURSE이면 null",
            example = "경유지 추가할게요"
        )
        String content,

        @Schema(
            description = "액션 코드. type=CHAT이면 CHAT_SENDED. type=COURSE이면 ROUTE_UPDATED | STOP_ADDED | STOP_REMOVED | LEG_UPDATED | TITLE_CHANGED | EDITING_COMPLETED | EDITING_RESUMED",
            example = "CHAT_SENDED",
            allowableValues = {"CHAT_SENDED", "ROUTE_UPDATED", "STOP_ADDED", "STOP_REMOVED", "LEG_UPDATED", "TITLE_CHANGED", "EDITING_COMPLETED", "EDITING_RESUMED"}
        )
        String action,

        @Schema(
            description = "사람이 읽기 쉬운 이벤트 설명. 예: '홍길동님이 루트를 수정했습니다'",
            example = "홍길동님이 메시지를 보냈습니다"
        )
        String editDescription,

        @Schema(
            description = "구체적인 변경 내용 (JSON 문자열). type=COURSE일 때만 존재할 수 있으며, 없으면 null. 예: {\"stopNames\":[\"신세계백화점\"]}",
            example = "{\"stopNames\":[\"신세계백화점\"]}"
        )
        String editDetails,

        @Schema(description = "이벤트 발생 시각", example = "2026-04-01T10:00:00")
        LocalDateTime createdAt
) {

    public static RouteHistoryFeedItem from(ChatMessage message, Profile profile) {
        String nickname = profile != null ? profile.getNickname() : message.getSender().getUserId();
        String profileImageUrl = profile != null ? profile.getProfileImageUrl() : null;
        boolean isCourseEvent = message.getType().isCourseEvent();

        return new RouteHistoryFeedItem(
                isCourseEvent ? "COURSE" : "CHAT",
                message.getId(),
                message.getSender().getUuid(),
                nickname,
                profileImageUrl,
                isCourseEvent ? null : message.getContent(),
                isCourseEvent ? message.getType().name() : "CHAT_SENDED",
                message.getType().describe(nickname),
                isCourseEvent ? message.getEditDetails() : null,
                message.getCreatedAt()
        );
    }
}
