package com.eodigaljido.backend.event;

import com.eodigaljido.backend.domain.notification.Notification;
import com.eodigaljido.backend.domain.notification.NotificationType;
import com.eodigaljido.backend.domain.user.User;
import com.eodigaljido.backend.repository.NotificationRepository;
import com.eodigaljido.backend.repository.UserRepository;
import com.eodigaljido.backend.service.NotificationSettingService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationEventListenerTest {

    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final NotificationSettingService settingService = mock(NotificationSettingService.class);
    private final NotificationEventListener listener = new NotificationEventListener(
            notificationRepository, userRepository, messagingTemplate, settingService);

    @Test
    void doesNotPersistOrSendDisabledNotification() {
        User recipient = User.builder().id(1L).uuid("recipient-uuid").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(recipient));
        when(settingService.isEnabled(recipient, NotificationType.CHAT_MESSAGE)).thenReturn(false);

        listener.handle(NotificationEvent.of(
                1L, null, NotificationType.CHAT_MESSAGE,
                "title", "body", "room-uuid", "CHAT_ROOM"));

        verify(notificationRepository, never()).save(any(Notification.class));
        verifyNoInteractions(messagingTemplate);
    }
}
