package com.eodigaljido.backend.event;

import com.eodigaljido.backend.domain.notification.NotificationType;

public record NotificationEvent(
        Long recipientId,
        Long senderId,
        NotificationType type,
        String title,
        String body,
        String referenceId,
        String referenceType
) {
    public static NotificationEvent of(Long recipientId, Long senderId,
                                       NotificationType type, String title, String body,
                                       String referenceId, String referenceType) {
        return new NotificationEvent(recipientId, senderId, type, title, body, referenceId, referenceType);
    }
}
