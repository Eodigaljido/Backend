package com.eodigaljido.backend.event;

import com.eodigaljido.backend.domain.notification.Notification;
import com.eodigaljido.backend.domain.user.User;
import com.eodigaljido.backend.dto.notification.NotificationResponse;
import com.eodigaljido.backend.repository.NotificationRepository;
import com.eodigaljido.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(NotificationEvent event) {
        User recipient = userRepository.findById(event.recipientId()).orElse(null);
        if (recipient == null) return;

        User sender = event.senderId() != null
                ? userRepository.findById(event.senderId()).orElse(null)
                : null;

        Notification notification = Notification.builder()
                .recipient(recipient)
                .sender(sender)
                .type(event.type())
                .title(event.title())
                .body(event.body())
                .referenceId(event.referenceId())
                .referenceType(event.referenceType())
                .build();

        Notification saved = notificationRepository.save(notification);

        // STOMP 사용자 식별자는 StompJwtChannelInterceptor에서 userId.toString()으로 설정됨
        messagingTemplate.convertAndSendToUser(
                String.valueOf(recipient.getId()),
                "/queue/notifications",
                NotificationResponse.from(saved)
        );
    }
}
