package com.eodigaljido.backend.service;

import com.eodigaljido.backend.config.JwtProperties;
import com.eodigaljido.backend.domain.user.PhoneVerification;
import com.eodigaljido.backend.domain.user.Profile;
import com.eodigaljido.backend.domain.user.RefreshToken;
import com.eodigaljido.backend.domain.user.User;
import com.eodigaljido.backend.dto.auth.*;
import com.eodigaljido.backend.exception.AuthException;
import com.eodigaljido.backend.repository.ProfileRepository;
import com.eodigaljido.backend.repository.RefreshTokenRepository;
import com.eodigaljido.backend.repository.UserRepository;
import com.eodigaljido.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final PasswordEncoder passwordEncoder;
    private final PhoneVerificationService phoneVerificationService;

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new AuthException("이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByPhone(request.phone())) {
            throw new AuthException("이미 사용 중인 전화번호입니다.", HttpStatus.CONFLICT);
        }
        if (profileRepository.existsByNickname(request.nickname())) {
            throw new AuthException("이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT);
        }

        // 전화번호 인증 완료 여부 확인
        if (!phoneVerificationService.checkVerified(request.phone(), PhoneVerification.Purpose.REGISTER)) {
            throw new AuthException("전화번호 인증이 완료되지 않았습니다.", HttpStatus.BAD_REQUEST);
        }

        User user = User.builder()
                .uuid(UUID.randomUUID().toString())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .phone(request.phone())
                .phoneVerifiedAt(LocalDateTime.now())
                .provider(User.Provider.LOCAL)
                .build();

        userRepository.save(user);

        Profile profile = Profile.builder()
                .user(user)
                .nickname(request.nickname())
                .build();

        profileRepository.save(profile);

        // 인증 플래그 제거
        phoneVerificationService.clearVerified(request.phone(), PhoneVerification.Purpose.REGISTER);

        return issueTokens(user, null, null);
    }

    @Transactional
    public LoginResponse login(LoginRequest request, String deviceInfo, String ipAddress) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AuthException("이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthException("이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED);
        }

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new AuthException("사용할 수 없는 계정입니다.", HttpStatus.FORBIDDEN);
        }

        user.updateLastLoginAt(LocalDateTime.now());
        return issueTokens(user, deviceInfo, ipAddress);
    }

    @Transactional
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        if (!jwtTokenProvider.isValid(request.refreshToken())) {
            throw new AuthException("유효하지 않은 refresh token입니다.", HttpStatus.UNAUTHORIZED);
        }

        String tokenHash = jwtTokenProvider.hashToken(request.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(tokenHash)
                .orElseThrow(() -> new AuthException("만료되었거나 폐기된 refresh token입니다.", HttpStatus.UNAUTHORIZED));

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            stored.revoke();
            throw new AuthException("만료된 refresh token입니다.", HttpStatus.UNAUTHORIZED);
        }

        User user = stored.getUser();
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole().name());
        long expiresIn = jwtProperties.getAccessTokenExpiry() / 1000;
        return TokenRefreshResponse.of(newAccessToken, expiresIn);
    }

    @Transactional
    public void logout(Long userId, String refreshToken) {
        if (!jwtTokenProvider.isValid(refreshToken)) {
            throw new AuthException("유효하지 않은 refresh token입니다.", HttpStatus.BAD_REQUEST);
        }
        String tokenHash = jwtTokenProvider.hashToken(refreshToken);
        refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(tokenHash)
                .ifPresent(RefreshToken::revoke);
    }

    @Transactional
    public void logoutAllDevices(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        refreshTokenRepository.findAllByUserAndRevokedAtIsNull(user)
                .forEach(RefreshToken::revoke);
    }

    @Transactional
    public void updatePhone(Long userId, String newPhone) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        if (userRepository.existsByPhone(newPhone)) {
            throw new AuthException("이미 사용 중인 전화번호입니다.", HttpStatus.CONFLICT);
        }

        // 인증 완료된 번호인지 확인
        boolean verified = phoneVerificationService.checkVerified(newPhone, PhoneVerification.Purpose.CHANGE_PHONE);
        if (!verified) {
            throw new AuthException("전화번호 인증이 완료되지 않았습니다.", HttpStatus.BAD_REQUEST);
        }

        user.updatePhone(newPhone, LocalDateTime.now());

        // 인증 플래그 제거
        phoneVerificationService.clearVerified(newPhone, PhoneVerification.Purpose.CHANGE_PHONE);
    }

    private LoginResponse issueTokens(User user, String deviceInfo, String ipAddress) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole().name());
        String refreshTokenStr = jwtTokenProvider.generateRefreshToken(user.getId());

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(jwtTokenProvider.hashToken(refreshTokenStr))
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();

        refreshTokenRepository.save(refreshToken);

        long expiresIn = jwtProperties.getAccessTokenExpiry() / 1000;
        String nickname = profileRepository.findByUser(user)
                .map(Profile::getNickname)
                .orElse(null);
        return LoginResponse.of(accessToken, refreshTokenStr, expiresIn, user, nickname);
    }
}
