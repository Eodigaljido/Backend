package com.eodigaljido.backend.dto.notification;

import com.eodigaljido.backend.domain.notification.Notification;
import com.eodigaljido.backend.domain.notification.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "알림 응답")
public record NotificationResponse(
        @Schema(description = "알림 ID") Long id,
        @Schema(
                description = """
                        알림 유형.
                        채팅, 친구, 코스, 모임에서 발생한 알림 종류를 나타냅니다.
                        """,
                example = "CHAT_MESSAGE"
        ) NotificationType type,
        @Schema(description = "알림 제목") String title,
        @Schema(description = "알림 내용") String body,
        @Schema(description = "관련 리소스 ID (roomUuid, routeUuid 등)") String referenceId,
        @Schema(description = "관련 리소스 유형 (USER, CHAT_ROOM, GROUP, GROUP_POST, ROUTE)") String referenceType,
        @Schema(description = "발신자 UUID") String senderUuid,
        @Schema(description = "발신자 userId") String senderUserId,
        @Schema(description = "읽음 여부") boolean isRead,
        @Schema(description = "생성 시각") LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getBody(),
                n.getReferenceId(),
                n.getReferenceType(),
                n.getSender() != null ? n.getSender().getUuid() : null,
                n.getSender() != null ? n.getSender().getUserId() : null,
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
