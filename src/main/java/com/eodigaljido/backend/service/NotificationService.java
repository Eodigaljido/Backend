package com.eodigaljido.backend.service;

import com.eodigaljido.backend.domain.notification.Notification;
import com.eodigaljido.backend.domain.user.User;
import com.eodigaljido.backend.dto.notification.NotificationResponse;
import com.eodigaljido.backend.exception.NotificationException;
import com.eodigaljido.backend.repository.NotificationRepository;
import com.eodigaljido.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    private static final int PAGE_SIZE = 10;

    public List<NotificationResponse> getNotifications(Long userId, int page) {
        User user = findUser(userId);
        return notificationRepository
                .findByRecipientOrderByCreatedAtDesc(user, PageRequest.of(page, PAGE_SIZE))
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    public long getUnreadCount(Long userId) {
        User user = findUser(userId);
        return notificationRepository.countByRecipientAndIsReadFalse(user);
    }

    @Transactional
    public NotificationResponse markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationException("알림을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (!notification.getRecipient().getId().equals(userId)) {
            throw new NotificationException("해당 알림에 접근할 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
        notification.markAsRead();
        return NotificationResponse.from(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        User user = findUser(userId);
        notificationRepository.markAllAsRead(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotificationException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }
}
