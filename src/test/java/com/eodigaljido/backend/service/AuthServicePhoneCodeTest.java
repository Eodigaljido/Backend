package com.eodigaljido.backend.service;

import com.eodigaljido.backend.config.JwtProperties;
import com.eodigaljido.backend.domain.user.PhoneVerification;
import com.eodigaljido.backend.exception.AuthException;
import com.eodigaljido.backend.repository.ProfileRepository;
import com.eodigaljido.backend.repository.RefreshTokenRepository;
import com.eodigaljido.backend.repository.UserRepository;
import com.eodigaljido.backend.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServicePhoneCodeTest {

    private UserRepository userRepository;
    private PhoneVerificationService phoneVerificationService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        phoneVerificationService = mock(PhoneVerificationService.class);
        authService = new AuthService(
                userRepository,
                mock(ProfileRepository.class),
                mock(RefreshTokenRepository.class),
                mock(JwtTokenProvider.class),
                mock(JwtProperties.class),
                mock(PasswordEncoder.class),
                phoneVerificationService,
                mock(FriendCodeService.class)
        );
    }

    @Test
    void rejectsRegisteredPhoneBeforeSendingChangePhoneCode() {
        assertDuplicatePhoneIsRejectedBeforeSending(PhoneVerification.Purpose.CHANGE_PHONE);
    }

    @Test
    void rejectsRegisteredPhoneBeforeSendingRegisterCode() {
        assertDuplicatePhoneIsRejectedBeforeSending(PhoneVerification.Purpose.REGISTER);
    }

    @Test
    void sendsCodeWhenPhoneIsAvailable() {
        String phone = "01012345678";
        when(userRepository.existsByPhone(phone)).thenReturn(false);

        authService.sendPhoneCode(phone, PhoneVerification.Purpose.CHANGE_PHONE);

        verify(phoneVerificationService).sendCode(phone, PhoneVerification.Purpose.CHANGE_PHONE);
    }

    private void assertDuplicatePhoneIsRejectedBeforeSending(PhoneVerification.Purpose purpose) {
        String phone = "01012345678";
        when(userRepository.existsByPhone(phone)).thenReturn(true);

        assertThatThrownBy(() -> authService.sendPhoneCode(phone, purpose))
                .isInstanceOfSatisfying(AuthException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getMessage()).isEqualTo("이미 사용중인 전화번호입니다.");
                });

        verify(phoneVerificationService, never()).sendCode(phone, purpose);
    }
}
