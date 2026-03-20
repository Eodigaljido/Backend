package com.eodigaljido.backend.controller;

import com.eodigaljido.backend.dto.auth.*;
import com.eodigaljido.backend.dto.common.ErrorResponse;
import com.eodigaljido.backend.service.AuthService;
import com.eodigaljido.backend.service.OAuthService;
import com.eodigaljido.backend.service.PhoneVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    @Operation(
            summary = "회원가입",
            description = """
                    이메일/비밀번호로 신규 회원을 등록합니다.

                    **사전 조건:** 요청 전에 아래 두 단계를 반드시 완료해야 합니다.
                    1. `POST /auth/phone/code` — phone, purpose=REGISTER 로 인증번호 발송
                    2. `POST /auth/phone/verify` — 수신한 6자리 코드 검증

                    인증 완료 후 10분 이내에 본 API를 호출해야 합니다.

                    **Request Body:**
                    - `email` (필수): 가입할 이메일 주소 (중복 불가)
                    - `password` (필수): 8~100자 비밀번호
                    - `nickname` (필수): 2~50자 닉네임 (중복 불가)
                    - `phone` (필수): 하이픈 없는 휴대폰 번호 (예: 01012345678), 인증 완료된 번호와 일치해야 함
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않음 (유효성 검사 실패 또는 전화번호 미인증)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 이메일, 전화번호 또는 닉네임",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(
            summary = "로그인",
            description = """
                    이메일/비밀번호로 로그인 후 access token과 refresh token을 발급합니다.

                    **Request Body:**
                    - `email` (필수): 가입한 이메일 주소
                    - `password` (필수): 비밀번호

                    **Response:**
                    - `accessToken`: API 인증에 사용하는 JWT (유효기간 1시간), 이후 요청의 `Authorization: Bearer {accessToken}` 헤더에 포함
                    - `refreshToken`: 만료된 access token 재발급용 JWT (유효기간 30일)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공, accessToken / refreshToken 반환"),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호가 일치하지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                        HttpServletRequest httpRequest) {
        String deviceInfo = httpRequest.getHeader("User-Agent");
        String ipAddress = httpRequest.getRemoteAddr();
        return ResponseEntity.ok(authService.login(request, deviceInfo, ipAddress));
    }

    @PostMapping("/token/refresh")
    @Operation(
            summary = "토큰 재발급",
            description = """
                    만료된 access token을 refresh token으로 재발급합니다.

                    **Request Body:**
                    - `refreshToken` (필수): 로그인 시 발급받은 refresh token (JWT 문자열 전체)

                    **Response:**
                    - `accessToken`: 새로 발급된 access token (유효기간 1시간)

                    refresh token이 만료되었거나 폐기된 경우 401을 반환합니다. 이 경우 재로그인이 필요합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공, 새 accessToken 반환"),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "refresh token이 만료되었거나 폐기됨 — 재로그인 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<TokenRefreshResponse> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "로그아웃",
            description = """
                    현재 디바이스의 refresh token을 폐기합니다 (RFC 7009).

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Request Body:**
                    - `refreshToken` (필수): 폐기할 refresh token (현재 디바이스에서 발급받은 JWT 문자열)

                    토큰이 폐기되면 해당 refresh token으로 더 이상 access token을 재발급받을 수 없습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> logout(@AuthenticationPrincipal UserDetails userDetails,
                                @Valid @RequestBody TokenRefreshRequest request) {
        authService.logout(Long.parseLong(userDetails.getUsername()), request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout/all-devices")
    @Operation(
            summary = "모든 디바이스 로그아웃",
            description = """
                    해당 계정에서 발급된 모든 refresh token을 폐기합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Request Body:** 없음

                    비밀번호 변경, 계정 탈취 의심 등의 상황에서 사용합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "모든 디바이스 로그아웃 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> logoutAllDevices(@AuthenticationPrincipal UserDetails userDetails) {
        authService.logoutAllDevices(Long.parseLong(userDetails.getUsername()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/oauth/google")
    @Operation(
            summary = "Google OAuth 로그인/회원가입",
            description = """
                    Google OAuth 인가 코드로 로그인하거나 신규 가입합니다.

                    **흐름:**
                    1. 프론트엔드가 Google 로그인 페이지로 이동
                    2. 사용자 동의 후 `redirect_uri`로 `code` 파라미터와 함께 리다이렉트
                    3. 해당 `code`를 본 API에 전달

                    **Request Body:**
                    - `code` (필수): Google 인가 코드 (redirect URI로 전달된 `code` 쿼리 파라미터 값)

                    **Response:**
                    - `accessToken` / `refreshToken` 발급 (신규 유저는 자동 가입 후 발급)
                    - `isNewUser`: true이면 신규 가입, false이면 기존 계정 로그인
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인/회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 Google 인가 코드",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<OAuthLoginResponse> googleOAuth(@Valid @RequestBody OAuthLoginRequest request) {
        return ResponseEntity.ok(oAuthService.loginWithGoogle(request.code(), request.redirectUri()));
    }

    @PostMapping("/oauth/kakao")
    @Operation(
            summary = "Kakao OAuth 로그인/회원가입",
            description = """
                    Kakao OAuth 인가 코드로 로그인하거나 신규 가입합니다.

                    **흐름:**
                    1. 프론트엔드가 Kakao 로그인 페이지로 이동
                    2. 사용자 동의 후 `redirect_uri`로 `code` 파라미터와 함께 리다이렉트
                    3. 해당 `code`를 본 API에 전달

                    **Request Body:**
                    - `code` (필수): Kakao 인가 코드 (redirect URI로 전달된 `code` 쿼리 파라미터 값)

                    **Response:**
                    - `accessToken` / `refreshToken` 발급 (신규 유저는 자동 가입 후 발급)
                    - `isNewUser`: true이면 신규 가입, false이면 기존 계정 로그인
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인/회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 Kakao 인가 코드",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<OAuthLoginResponse> kakaoOAuth(@Valid @RequestBody OAuthLoginRequest request) {
        return ResponseEntity.ok(oAuthService.loginWithKakao(request.code(), request.redirectUri()));
    }

    @PostMapping("/oauth/kakao/link")
    @Operation(
            summary = "카카오 계정 연동",
            description = """
                    이미 이메일(LOCAL)로 가입한 계정에 카카오 계정을 연동합니다.
                    연동 후에는 카카오 로그인으로도 이 계정에 접근할 수 있습니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Request Body:**
                    - `code` (필수): 카카오 인가 코드
                    - `redirectUri` (선택): 인가 코드 발급 시 사용한 redirect_uri (생략 시 서버 설정값 사용)

                    **제약 조건:**
                    - 이미 카카오로 가입한 계정은 연동 불필요 (400)
                    - 이미 연동된 계정은 중복 연동 불가 (409)
                    - 해당 카카오 계정이 다른 계정에 연결되어 있으면 불가 (409)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "카카오 연동 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 코드 또는 카카오 가입 계정",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 연동된 카카오 계정 있음 또는 해당 카카오 ID가 다른 계정에 연결됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "410", description = "이미 탈퇴한 계정",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> linkKakao(@AuthenticationPrincipal UserDetails userDetails,
                                   @Valid @RequestBody KakaoLinkRequest request) {
        oAuthService.linkKakao(Long.valueOf(userDetails.getUsername()), request.code(), request.redirectUri());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/oauth/kakao/link")
    @Operation(
            summary = "카카오 계정 연동 해제",
            description = """
                    연동된 카카오 계정을 해제합니다.
                    해제 후에는 카카오 로그인으로 이 계정에 접근할 수 없습니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **제약 조건:**
                    - 카카오로 가입한 계정은 연동 해제 불가 (400)
                    - 연동된 카카오 계정이 없으면 오류 (400)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "카카오 연동 해제 성공"),
            @ApiResponse(responseCode = "400", description = "연동된 카카오 계정 없음 또는 카카오 가입 계정",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "410", description = "이미 탈퇴한 계정",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> unlinkKakao(@AuthenticationPrincipal UserDetails userDetails) {
        oAuthService.unlinkKakao(Long.valueOf(userDetails.getUsername()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/phone/code")
    @Operation(
            summary = "SMS 인증번호 발송",
            description = """
                    입력한 휴대폰 번호로 6자리 인증번호를 SMS로 발송합니다.

                    **Request Body:**
                    - `phone` (필수): 인증번호를 받을 휴대폰 번호, 하이픈 없이 입력 (예: 01012345678)
                    - `purpose` (필수): 인증 목적
                      - `REGISTER` — 회원가입 시 사용
                      - `CHANGE_PHONE` — 전화번호 변경 시 사용

                    인증번호는 **3분간** 유효하며, 동일 번호로 재발송 시 이전 코드는 무효가 됩니다.
                    최대 **5회** 오입력 시 코드가 잠기며 재발송이 필요합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SMS 발송 성공, 유효 시간(초) 반환"),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<PhoneCodeResponse> sendPhoneCode(@Valid @RequestBody PhoneCodeRequest request) {
        phoneVerificationService.sendCode(request.phone(), request.purpose());
        return ResponseEntity.ok(PhoneCodeResponse.of(180));
    }

    @PostMapping("/phone/verify")
    @Operation(
            summary = "SMS 인증번호 검증",
            description = """
                    SMS로 수신한 인증번호를 검증합니다.

                    **Request Body:**
                    - `phone` (필수): 인증번호를 발송한 휴대폰 번호 (하이픈 없이, 예: 01012345678)
                    - `code` (필수): SMS로 수신한 6자리 숫자 인증번호 (예: 123456)
                    - `purpose` (필수): 인증 목적 (`REGISTER` 또는 `CHANGE_PHONE`), 발송 시 사용한 값과 동일해야 함

                    검증 성공 시 **10분간** 인증 완료 상태가 유지됩니다.
                    이 시간 내에 회원가입(`/auth/register`) 또는 전화번호 변경(`/users/me/phone`)을 완료해야 합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "인증 성공"),
            @ApiResponse(responseCode = "400", description = "인증번호가 틀렸거나 만료됨, 또는 시도 횟수 초과",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    ResponseEntity<Void> verifyPhoneCode(@Valid @RequestBody PhoneVerifyRequest request) {
        phoneVerificationService.verifyCode(request.phone(), request.code(), request.purpose());
        return ResponseEntity.noContent().build();
    }
}
