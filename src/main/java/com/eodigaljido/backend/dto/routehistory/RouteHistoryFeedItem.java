package com.eodigaljido.backend.dto.routehistory;

import com.eodigaljido.backend.domain.chat.ChatMessage;
import com.eodigaljido.backend.domain.route.RouteEditLog;
import com.eodigaljido.backend.domain.user.Profile;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "루트 기록 피드 항목 (채팅 메시지 또는 루트 수정 이벤트)")
public record RouteHistoryFeedItem(

        @Schema(description = "항목 타입: CHAT | EDIT", example = "CHAT")
        String type,

        @Schema(description = "항목 고유 ID (타입+id 조합)", example = "CHAT:550e8400-e29b-41d4-a716-446655440001")
        String itemId,

        @Schema(description = "작성자 UUID", example = "770e8400-e29b-41d4-a716-446655440002")
        String actorUuid,

        @Schema(description = "작성자 닉네임", example = "홍길동")
        String actorNickname,

        @Schema(description = "작성자 프로필 이미지 URL", example = "https://example.com/profile.jpg")
        String actorProfileImageUrl,

        @Schema(description = "채팅 메시지 내용 (type=CHAT일 때)", example = "경유지 추가할게요")
        String content,

        @Schema(description = "루트 수정 액션 코드 (type=EDIT일 때)", example = "ADD_WAYPOINT")
        String action,

        @Schema(description = "루트 수정 설명 (type=EDIT일 때)", example = "홍길동님이 경유지를 추가했습니다")
        String editDescription,

        @Schema(description = "발생 시각", example = "2026-04-01T10:00:00")
        LocalDateTime createdAt
) {

    public static RouteHistoryFeedItem fromChatMessage(ChatMessage message, Profile profile) {
        String nickname = profile != null ? profile.getNickname() : message.getSender().getUserId();
        String profileImageUrl = profile != null ? profile.getProfileImageUrl() : null;
        return new RouteHistoryFeedItem(
                "CHAT",
                "CHAT:" + message.getUuid(),
                message.getSender().getUuid(),
                nickname,
                profileImageUrl,
                message.getContent(),
                null,
                null,
                message.getCreatedAt()
        );
    }

    public static RouteHistoryFeedItem fromEditLog(RouteEditLog log, Profile profile) {
        String nickname = profile != null ? profile.getNickname() : log.getEditor().getUserId();
        String profileImageUrl = profile != null ? profile.getProfileImageUrl() : null;
        String description = buildEditDescription(nickname, log.getAction());
        return new RouteHistoryFeedItem(
                "EDIT",
                "EDIT:" + log.getId(),
                log.getEditor().getUuid(),
                nickname,
                profileImageUrl,
                null,
                log.getAction().name(),
                description,
                log.getCreatedAt()
        );
    }

    private static String buildEditDescription(String nickname, RouteEditLog.EditAction action) {
        String suffix = switch (action) {
            case ADD_WAYPOINT -> "님이 경유지를 추가했습니다";
            case UPDATE_WAYPOINT -> "님이 경유지를 수정했습니다";
            case REMOVE_WAYPOINT -> "님이 경유지를 삭제했습니다";
            case ADD_LEG -> "님이 이동 경로를 추가했습니다";
            case UPDATE_LEG -> "님이 이동 경로를 수정했습니다";
            case REMOVE_LEG -> "님이 이동 경로를 삭제했습니다";
            case UPDATE_TITLE -> "님이 루트 제목을 변경했습니다";
            case UPDATE_DESCRIPTION -> "님이 루트 설명을 변경했습니다";
            case UPDATE_THUMBNAIL -> "님이 대표 이미지를 변경했습니다";
            case UPDATE_TAGS -> "님이 태그를 변경했습니다";
            case UPDATE_REGION -> "님이 지역을 변경했습니다";
            case UPDATE_ACTIVITY_TYPE -> "님이 활동 유형을 변경했습니다";
        };
        return nickname + suffix;
    }
}
