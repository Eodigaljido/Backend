package com.eodigaljido.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eodigaljido.backend.domain.user.User;
import com.eodigaljido.backend.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class CustomUserDetailsServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final CustomUserDetailsService service = new CustomUserDetailsService(userRepository);

    @Test
    void loadUserByUsernameReturnsActiveUserOnly() {
        User user = User.builder()
                .id(1L)
                .uuid("550e8400-e29b-41d4-a716-446655440000")
                .role(User.Role.USER)
                .status(User.UserStatus.ACTIVE)
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThat(service.loadUserByUsername("1").getUsername()).isEqualTo("1");
    }

    @Test
    void loadUserByUsernameRejectsDeletedUser() {
        User user = User.builder()
                .id(1L)
                .uuid("550e8400-e29b-41d4-a716-446655440000")
                .role(User.Role.USER)
                .status(User.UserStatus.DELETED)
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.loadUserByUsername("1"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void loadUserByUsernameRejectsInvalidId() {
        assertThatThrownBy(() -> service.loadUserByUsername("not-a-number"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
