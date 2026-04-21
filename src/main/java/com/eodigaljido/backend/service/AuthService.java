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
import java.util.Optional;
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
        String userId = (request.userId() != null && !request.userId().isBlank()) ? request.userId() : null;
        String email  = (request.email()  != null && !request.email().isBlank())  ? request.email()  : null;

        if (userId == null && email == null) {
            throw new AuthException("아이디 또는 이메일 중 하나는 필수입니다.", HttpStatus.BAD_REQUEST);
        }

        if (userId != null && userRepository.existsByUserId(userId)) {
            throw new AuthException("이미 사용 중인 아이디입니다.", HttpStatus.CONFLICT);
        }

        // 동일 이메일로 OAuth 계정이 이미 있으면 LOCAL 비밀번호를 추가(연동)
        if (email != null) {
            Optional<User> existingByEmail = userRepository.findByEmail(email);
            if (existingByEmail.isPresent()) {
                User existing = existingByEmail.get();
                if (existing.getPasswordHash() != null) {
                    throw new AuthException("이미 이메일/비밀번호로 가입된 계정입니다.", HttpStatus.CONFLICT);
                }
                if (profileRepository.existsByNicknameAndUserNot(request.nickname(), existing)) {
                    throw new AuthException("이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT);
                }
                if (userId != null) existing.updateUserId(userId);
                existing.linkLocalPassword(passwordEncoder.encode(request.password()));
                profileRepository.findByUser(existing).ifPresent(p -> p.updateNickname(request.nickname()));
                return issueTokens(existing, null, null);
            }
        }

        if (profileRepository.existsByNickname(request.nickname())) {
            throw new AuthException("이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .uuid(UUID.randomUUID().toString())
                .userId(userId)
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();

        userRepository.save(user);
        profileRepository.save(Profile.builder().user(user).nickname(request.nickname()).build());

        return issueTokens(user, null, null);
    }

    @Transactional
    public void registerPhone(Long userId, String phone) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        if (user.getPhone() != null) {
            throw new AuthException("이미 전화번호가 등록된 계정입니다.", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByPhone(phone)) {
            throw new AuthException("이미 사용 중인 전화번호입니다.", HttpStatus.CONFLICT);
        }
        if (!phoneVerificationService.checkVerified(phone, PhoneVerification.Purpose.REGISTER)) {
            throw new AuthException("전화번호 인증이 완료되지 않았습니다.", HttpStatus.BAD_REQUEST);
        }

        user.updatePhone(phone, LocalDateTime.now());
        phoneVerificationService.clearVerified(phone, PhoneVerification.Purpose.REGISTER);
    }

    @Transactional
    public LoginResponse login(LoginRequest request, String deviceInfo, String ipAddress) {
        String identifier = request.identifier();
        User user = (identifier.contains("@")
                ? userRepository.findByEmail(identifier)
                : userRepository.findByUserId(identifier))
                .orElseThrow(() -> new AuthException("아이디/이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED));

        if (user.getPasswordHash() == null) {
            throw new AuthException("소셜 로그인으로 가입된 계정입니다. 구글 또는 카카오 로그인을 이용해주세요.", HttpStatus.UNAUTHORIZED);
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthException("아이디/이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED);
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
