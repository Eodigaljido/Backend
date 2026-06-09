package com.eodigaljido.backend.dto.routehistory;

import com.eodigaljido.backend.domain.route.RouteHistoryLog;
import com.eodigaljido.backend.domain.user.Profile;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "루트 기록 피드 항목")
public record RouteHistoryFeedItem(

        @Schema(
            description = "이벤트 타입. CHAT: 메시지 전송, EDIT: 수정/삭제/루트편집",
            example = "CHAT",
            allowableValues = {"CHAT", "EDIT"}
        )
        String type,

        @Schema(description = "이벤트 고유 ID (route_history_logs PK)", example = "42")
        Long itemId,

        @Schema(description = "이벤트 발생 유저 UUID", example = "770e8400-e29b-41d4-a716-446655440002")
        String actorUuid,

        @Schema(description = "이벤트 발생 유저 닉네임", example = "홍길동")
        String actorNickname,

        @Schema(description = "이벤트 발생 유저 프로필 이미지 URL (없으면 null)", example = "https://example.com/profile.jpg")
        String actorProfileImageUrl,

        @Schema(
            description = "메시지 내용. type=CHAT이면 전송 시점 원본 내용, action=CHAT_EDITED이면 수정 후 내용, 그 외 null",
            example = "경유지 추가할게요"
        )
        String content,

        @Schema(
            description = "편집 액션 코드. type=EDIT일 때만 존재. ROUTE_UPDATED | CHAT_EDITED | CHAT_DELETED",
            example = "ROUTE_UPDATED",
            allowableValues = {"ROUTE_UPDATED", "CHAT_EDITED", "CHAT_DELETED"}
        )
        String action,

        @Schema(
            description = "사람이 읽기 쉬운 편집 설명. type=EDIT일 때만 존재. 예: '홍길동님이 메시지를 삭제했습니다'",
            example = "홍길동님이 루트를 수정했습니다"
        )
        String editDescription,

        @Schema(description = "이벤트 발생 시각", example = "2026-04-01T10:00:00")
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
