package com.eodigaljido.backend.service;

import com.eodigaljido.backend.domain.notification.NotificationSetting;
import com.eodigaljido.backend.domain.notification.NotificationSettingKey;
import com.eodigaljido.backend.domain.notification.NotificationType;
import com.eodigaljido.backend.domain.user.User;
import com.eodigaljido.backend.exception.NotificationException;
import com.eodigaljido.backend.repository.NotificationSettingRepository;
import com.eodigaljido.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationSettingServiceTest {

    private final NotificationSettingRepository settingRepository = mock(NotificationSettingRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final NotificationSettingService service =
            new NotificationSettingService(settingRepository, userRepository);
    private final List<NotificationSetting> storedSettings = new ArrayList<>();
    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).uuid("user-uuid").build();
        storedSettings.clear();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(settingRepository.findByUser(user)).thenAnswer(ignored -> List.copyOf(storedSettings));
        when(settingRepository.findByUserAndSettingKey(any(), any())).thenAnswer(invocation -> {
            NotificationSettingKey key = invocation.getArgument(1);
            return storedSettings.stream().filter(setting -> setting.getSettingKey() == key).findFirst();
        });
        when(settingRepository.save(any())).thenAnswer(invocation -> {
            NotificationSetting setting = invocation.getArgument(0);
            if (!storedSettings.contains(setting)) {
                storedSettings.add(setting);
            }
            return setting;
        });
    }

    @Test
    void returnsAllSettingsEnabledByDefault() {
        Map<String, Boolean> result = service.getSettings(1L);

        assertThat(result).hasSize(15);
        assertThat(result.values()).containsOnly(true);
    }

    @Test
    void partiallyUpdatesOneSettingAndReturnsFullMap() {
        Map<String, Boolean> result = service.updateSettings(1L, Map.of("chatMessage", false));

        assertThat(result).hasSize(15);
        assertThat(result.get("chatMessage")).isFalse();
        assertThat(result.get("friendRequest")).isTrue();
        assertThat(service.isEnabled(user, NotificationType.CHAT_MESSAGE)).isFalse();
    }

    @Test
    void rejectsUnknownSettingKey() {
        assertThatThrownBy(() -> service.updateSettings(1L, Map.of("unknownKey", false)))
                .isInstanceOf(NotificationException.class)
                .hasMessageContaining("unknownKey");
    }
}
