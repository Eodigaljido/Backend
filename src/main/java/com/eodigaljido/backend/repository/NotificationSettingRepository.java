package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.notification.NotificationSetting;
import com.eodigaljido.backend.domain.notification.NotificationSettingKey;
import com.eodigaljido.backend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
    List<NotificationSetting> findByUser(User user);
    Optional<NotificationSetting> findByUserAndSettingKey(User user, NotificationSettingKey settingKey);
}
