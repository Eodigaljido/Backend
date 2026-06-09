package com.eodigaljido.backend.service;

import com.eodigaljido.backend.domain.notification.NotificationSetting;
import com.eodigaljido.backend.domain.notification.NotificationSettingKey;
import com.eodigaljido.backend.domain.notification.NotificationType;
import com.eodigaljido.backend.domain.user.User;
import com.eodigaljido.backend.exception.NotificationException;
import com.eodigaljido.backend.repository.NotificationSettingRepository;
import com.eodigaljido.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationSettingService {

    private final NotificationSettingRepository notificationSettingRepository;
    private final UserRepository userRepository;

    public Map<String, Boolean> getSettings(Long userId) {
        return getSettings(findUser(userId));
    }

    public boolean isEnabled(User user, NotificationType type) {
        return NotificationSettingKey.fromNotificationType(type)
                .flatMap(key -> notificationSettingRepository.findByUserAndSettingKey(user, key))
                .map(NotificationSetting::isEnabled)
                .orElse(true);
    }

    @Transactional
    public Map<String, Boolean> updateSettings(Long userId, Map<String, Boolean> updates) {
        User user = findUser(userId);
        if (updates == null || updates.isEmpty()) {
            throw new NotificationException("변경할 알림 설정을 하나 이상 보내주세요.", HttpStatus.BAD_REQUEST);
        }

        updates.forEach((key, enabled) -> {
            NotificationSettingKey settingKey = NotificationSettingKey.fromKey(key)
                    .orElseThrow(() -> new NotificationException(
                            "지원하지 않는 알림 설정 key입니다: " + key, HttpStatus.BAD_REQUEST));
            if (enabled == null) {
                throw new NotificationException(
                        "알림 설정 값은 true 또는 false여야 합니다: " + key, HttpStatus.BAD_REQUEST);
            }

            NotificationSetting setting = notificationSettingRepository.findByUserAndSettingKey(user, settingKey)
                    .orElseGet(() -> NotificationSetting.builder()
                            .user(user)
                            .settingKey(settingKey)
                            .build());
            setting.updateEnabled(enabled);
            notificationSettingRepository.save(setting);
        });

        return getSettings(user);
    }

    private Map<String, Boolean> getSettings(User user) {
        Map<NotificationSettingKey, Boolean> stored = new EnumMap<>(NotificationSettingKey.class);
        notificationSettingRepository.findByUser(user)
                .forEach(setting -> stored.put(setting.getSettingKey(), setting.isEnabled()));

        Map<String, Boolean> result = new LinkedHashMap<>();
        for (NotificationSettingKey key : NotificationSettingKey.values()) {
            result.put(key.key(), stored.getOrDefault(key, true));
        }
        return result;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotificationException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }
}
