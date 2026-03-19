package com.eodigaljido.backend.controller;

import com.eodigaljido.backend.dto.auth.*;
import com.eodigaljido.backend.service.AuthService;
import com.eodigaljido.backend.service.OAuthService;
import com.eodigaljido.backend.service.PhoneVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "로그인 / 인증 API")
public class AuthController {

    private final AuthService authService;
    private final OAuthService oAuthService;
    private final PhoneVerificationService phoneVerificationService;

    @PostMapping("/register")
    @Operation(summary = "회원가입")
    ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인 후 access/refresh 토큰 발급")
    ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                        HttpServletRequest httpRequest) {
        String deviceInfo = httpRequest.getHeader("User-Agent");
        String ipAddress = httpRequest.getRemoteAddr();
        return ResponseEntity.ok(authService.login(request, deviceInfo, ipAddress));
    }

    @PostMapping("/token/refresh")
    @Operation(summary = "토큰 재발급", description = "refresh token으로 access token 재발급")
    ResponseEntity<TokenRefreshResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 디바이스의 refresh token 폐기 (RFC 7009)")
    ResponseEntity<Void> logout(@AuthenticationPrincipal UserDetails userDetails,
                                @Valid @RequestBody TokenRefreshRequest request) {
        authService.logout(Long.parseLong(userDetails.getUsername()), request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout/all-devices")
    @Operation(summary = "모든 디바이스 로그아웃", description = "해당 계정의 모든 refresh token 폐기")
    ResponseEntity<Void> logoutAllDevices(@AuthenticationPrincipal UserDetails userDetails) {
        authService.logoutAllDevices(Long.parseLong(userDetails.getUsername()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/oauth/google")
    @Operation(summary = "Google OAuth 로그인/회원가입")
    ResponseEntity<LoginResponse> googleOAuth(@Valid @RequestBody OAuthLoginRequest request) {
        return ResponseEntity.ok(oAuthService.loginWithGoogle(request.code()));
    }

    @PostMapping("/oauth/kakao")
    @Operation(summary = "Kakao OAuth 로그인/회원가입")
    ResponseEntity<LoginResponse> kakaoOAuth(@Valid @RequestBody OAuthLoginRequest request) {
        return ResponseEntity.ok(oAuthService.loginWithKakao(request.code()));
    }

    @PostMapping("/phone/code")
    @Operation(summary = "SMS 인증번호 발송", description = "6자리 인증번호를 SOLAPI를 통해 발송")
    ResponseEntity<Void> sendPhoneCode(@Valid @RequestBody PhoneCodeRequest request) {
        phoneVerificationService.sendCode(request.phone(), request.purpose());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/phone/verify")
    @Operation(summary = "SMS 인증번호 검증")
    ResponseEntity<Void> verifyPhoneCode(@Valid @RequestBody PhoneVerifyRequest request) {
        phoneVerificationService.verifyCode(request.phone(), request.code(), request.purpose());
        return ResponseEntity.noContent().build();
    }
}
