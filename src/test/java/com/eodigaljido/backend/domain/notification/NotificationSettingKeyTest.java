package com.eodigaljido.backend.domain.notification;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationSettingKeyTest {

    @Test
    void mapsNotificationTypesToFrontendSettingKeys() {
        assertThat(NotificationSettingKey.fromNotificationType(NotificationType.CHAT_MESSAGE))
                .contains(NotificationSettingKey.CHAT_MESSAGE);
        assertThat(NotificationSettingKey.fromNotificationType(NotificationType.FRIEND_ACCEPTED))
                .contains(NotificationSettingKey.FRIEND_ACCEPTED);
        assertThat(NotificationSettingKey.fromNotificationType(NotificationType.GROUP_JOIN_REJECTED))
                .contains(NotificationSettingKey.MEET_JOIN_RESULT);
        assertThat(NotificationSettingKey.fromNotificationType(NotificationType.ROUTE_USED))
                .contains(NotificationSettingKey.COURSE_FAVORITED_OR_USED);
        assertThat(NotificationSettingKey.fromNotificationType(NotificationType.GROUP_INVITED))
                .isEmpty();
    }
}
