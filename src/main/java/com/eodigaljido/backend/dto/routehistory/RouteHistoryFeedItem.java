package com.eodigaljido.backend.dto.routehistory;

import com.eodigaljido.backend.domain.route.RouteHistoryLog;
import com.eodigaljido.backend.domain.user.Profile;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "루트 기록 피드 항목 (채팅 메시지 또는 루트 수정 이벤트)")
public record RouteHistoryFeedItem(

        @Schema(description = "항목 타입: CHAT | EDIT", example = "CHAT")
        String type,

        @Schema(description = "항목 고유 ID", example = "42")
        Long itemId,

        @Schema(description = "작성자 UUID", example = "770e8400-e29b-41d4-a716-446655440002")
        String actorUuid,

        @Schema(description = "작성자 닉네임", example = "홍길동")
        String actorNickname,

        @Schema(description = "작성자 프로필 이미지 URL", example = "https://example.com/profile.jpg")
        String actorProfileImageUrl,

        @Schema(description = "채팅 메시지 내용 (type=CHAT일 때)", example = "경유지 추가할게요")
        String content,

        @Schema(description = "루트 수정 액션 코드 (type=EDIT일 때)", example = "ROUTE_UPDATED")
        String action,

        @Schema(description = "루트 수정 설명 (type=EDIT일 때)", example = "홍길동님이 루트를 수정했습니다")
        String editDescription,

        @Schema(description = "발생 시각", example = "2026-04-01T10:00:00")
        LocalDateTime createdAt
) {

    public static RouteHistoryFeedItem from(RouteHistoryLog log, Profile profile) {
        String nickname = profile != null ? profile.getNickname() : log.getActor().getUserId();
        String profileImageUrl = profile != null ? profile.getProfileImageUrl() : null;

        if (log.getType() == RouteHistoryLog.LogType.CHAT) {
            return new RouteHistoryFeedItem(
                    "CHAT",
                    log.getId(),
                    log.getActor().getUuid(),
                    nickname,
                    profileImageUrl,
                    log.getContent(),
                    null,
                    null,
                    log.getCreatedAt()
            );
        } else {
            String description = buildEditDescription(nickname, log.getEditAction());
            return new RouteHistoryFeedItem(
                    "EDIT",
                    log.getId(),
                    log.getActor().getUuid(),
                    nickname,
                    profileImageUrl,
                    null,
                    log.getEditAction(),
                    description,
                    log.getCreatedAt()
            );
        }
    }

    private static String buildEditDescription(String nickname, String action) {
        if (action == null) return nickname + "님이 루트를 수정했습니다";
        String suffix = switch (action) {
            case "ADD_WAYPOINT" -> "님이 경유지를 추가했습니다";
            case "UPDATE_WAYPOINT" -> "님이 경유지를 수정했습니다";
            case "REMOVE_WAYPOINT" -> "님이 경유지를 삭제했습니다";
            case "ADD_LEG" -> "님이 이동 경로를 추가했습니다";
            case "UPDATE_LEG" -> "님이 이동 경로를 수정했습니다";
            case "REMOVE_LEG" -> "님이 이동 경로를 삭제했습니다";
            case "UPDATE_TITLE" -> "님이 루트 제목을 변경했습니다";
            case "UPDATE_DESCRIPTION" -> "님이 루트 설명을 변경했습니다";
            case "UPDATE_THUMBNAIL" -> "님이 대표 이미지를 변경했습니다";
            case "UPDATE_TAGS" -> "님이 태그를 변경했습니다";
            case "UPDATE_REGION" -> "님이 지역을 변경했습니다";
            case "UPDATE_ACTIVITY_TYPE" -> "님이 활동 유형을 변경했습니다";
            case "CHAT_EDITED" -> "님이 메시지를 수정했습니다";
            case "CHAT_DELETED" -> "님이 메시지를 삭제했습니다";
            default -> "님이 루트를 수정했습니다";
        };
        return nickname + suffix;
    }
}
