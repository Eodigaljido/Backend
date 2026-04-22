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
                if (userId != null) existing.updateUserId(userId);
                existing.linkLocalPassword(passwordEncoder.encode(request.password()));
                return issueTokens(existing, null, null);
            }
        }

        User user = User.builder()
                .uuid(UUID.randomUUID().toString())
                .userId(userId)
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .build();

        userRepository.save(user);
        profileRepository.save(Profile.builder().user(user).nickname(generateDefaultNickname(userId, email)).build());

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

    // ── 아이디/이메일 찾기 ────────────────────────────────────────────────────

    public void sendFindAccountCode(String phone) {
        if (!userRepository.existsByPhone(phone)) {
            throw new AuthException("해당 전화번호로 가입된 계정이 없습니다.", HttpStatus.NOT_FOUND);
        }
        phoneVerificationService.sendCode(phone, PhoneVerification.Purpose.FIND_ACCOUNT);
    }

    public FindAccountResponse verifyFindAccountCode(String phone, String code) {
        phoneVerificationService.verifyCode(phone, code, PhoneVerification.Purpose.FIND_ACCOUNT);
        phoneVerificationService.clearVerified(phone, PhoneVerification.Purpose.FIND_ACCOUNT);
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new AuthException("계정을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        return FindAccountResponse.of(user.getUserId(), user.getEmail());
    }

    // ── 비밀번호 재설정 ───────────────────────────────────────────────────────

    public void sendResetPasswordCode(String identifier, String phone) {
        User user = userRepository.findByUserId(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new AuthException("아이디 또는 이메일에 해당하는 계정이 없습니다.", HttpStatus.NOT_FOUND));
        if (!phone.equals(user.getPhone())) {
            throw new AuthException("입력한 전화번호가 해당 계정에 등록된 전화번호와 일치하지 않습니다.", HttpStatus.BAD_REQUEST);
        }
        phoneVerificationService.sendCode(phone, PhoneVerification.Purpose.RESET_PASSWORD);
    }

    @Transactional
    public void verifyResetPasswordCode(String phone, String code, String newPassword) {
        phoneVerificationService.verifyCode(phone, code, PhoneVerification.Purpose.RESET_PASSWORD);
        phoneVerificationService.clearVerified(phone, PhoneVerification.Purpose.RESET_PASSWORD);
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new AuthException("계정을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        user.linkLocalPassword(passwordEncoder.encode(newPassword));
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

    private String generateDefaultNickname(String userId, String email) {
        String base = (userId != null) ? userId
                : email.substring(0, email.indexOf('@'));
        String candidate = base;
        int suffix = 1;
        while (profileRepository.existsByNickname(candidate)) {
            candidate = base + suffix++;
        }
        return candidate;
    }
}
