package com.eodigaljido.backend.service;

import com.eodigaljido.backend.config.JwtProperties;
import com.eodigaljido.backend.config.OAuthProperties;
import com.eodigaljido.backend.domain.user.Profile;
import com.eodigaljido.backend.domain.user.RefreshToken;
import com.eodigaljido.backend.domain.user.User;
import com.eodigaljido.backend.domain.user.UserOAuthProvider;
import com.eodigaljido.backend.domain.user.UserOAuthProvider.OAuthProvider;
import com.eodigaljido.backend.dto.auth.OAuthLoginResponse;
import com.eodigaljido.backend.exception.AuthException;
import com.eodigaljido.backend.repository.ProfileRepository;
import com.eodigaljido.backend.repository.RefreshTokenRepository;
import com.eodigaljido.backend.repository.UserOAuthProviderRepository;
import com.eodigaljido.backend.repository.UserRepository;
import com.eodigaljido.backend.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthService {

    private final OAuthProperties oAuthProperties;
    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserOAuthProviderRepository oAuthProviderRepository;
    private final JwtTokenProvider jwtTokenProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public OAuthLoginResponse loginWithGoogle(String authorizationCode, String redirectUri) {
        String effectiveRedirectUri = resolveAndValidateRedirectUri(redirectUri, oAuthProperties.getGoogle());
        String accessToken = exchangeGoogleCode(authorizationCode, effectiveRedirectUri);
        JsonNode userInfo = getGoogleUserInfo(accessToken);

        String providerId = userInfo.get("sub").asText();
        String email = userInfo.path("email").asText(null);
        String name = userInfo.path("name").asText("Google User");

        return oAuthProviderRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, providerId)
                .map(oap -> issueTokens(oap.getUser(), false))
                .orElseGet(() -> findOrCreateUser(OAuthProvider.GOOGLE, providerId, email, name));
    }

    @Transactional
    public OAuthLoginResponse loginWithKakao(String authorizationCode, String redirectUri) {
        String effectiveRedirectUri = resolveAndValidateRedirectUri(redirectUri, oAuthProperties.getKakao());
        String accessToken = exchangeKakaoCode(authorizationCode, effectiveRedirectUri);
        JsonNode userInfo = getKakaoUserInfo(accessToken);

        String providerId = userInfo.get("id").asText();
        JsonNode account = userInfo.path("kakao_account");
        String email = account.path("email").asText(null);
        String name = account.path("profile").path("nickname").asText("Kakao User");

        return oAuthProviderRepository.findByProviderAndProviderId(OAuthProvider.KAKAO, providerId)
                .map(oap -> issueTokens(oap.getUser(), false))
                .orElseGet(() -> findOrCreateUser(OAuthProvider.KAKAO, providerId, email, name));
    }

    @Transactional
    public void linkGoogle(Long userId, String authorizationCode, String redirectUri) {
        User user = getActiveUser(userId);

        if (oAuthProviderRepository.existsByUserAndProvider(user, OAuthProvider.GOOGLE)) {
            throw new AuthException("이미 구글 계정이 연동되어 있습니다.", HttpStatus.CONFLICT);
        }

        String effectiveRedirectUri = resolveAndValidateRedirectUri(redirectUri, oAuthProperties.getGoogle());
        String googleAccessToken = exchangeGoogleCode(authorizationCode, effectiveRedirectUri);
        String googleId = getGoogleUserInfo(googleAccessToken).get("sub").asText();

        if (oAuthProviderRepository.existsByProviderAndProviderId(OAuthProvider.GOOGLE, googleId)) {
            throw new AuthException("해당 구글 계정은 이미 다른 계정에 연결되어 있습니다.", HttpStatus.CONFLICT);
        }

        oAuthProviderRepository.save(UserOAuthProvider.of(user, OAuthProvider.GOOGLE, googleId));
    }

    @Transactional
    public void unlinkGoogle(Long userId) {
        User user = getActiveUser(userId);

        UserOAuthProvider oauthLink = oAuthProviderRepository.findByUserAndProvider(user, OAuthProvider.GOOGLE)
                .orElseThrow(() -> new AuthException("연동된 구글 계정이 없습니다.", HttpStatus.BAD_REQUEST));

        if (!user.isLocal() && oAuthProviderRepository.countByUser(user) <= 1) {
            throw new AuthException("마지막 로그인 수단은 해제할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }

        oAuthProviderRepository.delete(oauthLink);
    }


    @Transactional
    public void linkKakao(Long userId, String authorizationCode, String redirectUri) {
        User user = getActiveUser(userId);

        if (oAuthProviderRepository.existsByUserAndProvider(user, OAuthProvider.KAKAO)) {
            throw new AuthException("이미 카카오 계정이 연동되어 있습니다.", HttpStatus.CONFLICT);
        }

        String effectiveRedirectUri = resolveAndValidateRedirectUri(redirectUri, oAuthProperties.getKakao());
        String kakaoAccessToken = exchangeKakaoCode(authorizationCode, effectiveRedirectUri);
        String kakaoId = getKakaoUserInfo(kakaoAccessToken).get("id").asText();

        if (oAuthProviderRepository.existsByProviderAndProviderId(OAuthProvider.KAKAO, kakaoId)) {
            throw new AuthException("해당 카카오 계정은 이미 다른 계정에 연결되어 있습니다.", HttpStatus.CONFLICT);
        }

        oAuthProviderRepository.save(UserOAuthProvider.of(user, OAuthProvider.KAKAO, kakaoId));
    }

    // ── 카카오 연동 해제 ─────────────────────────────────────────────────────

    @Transactional
    public void unlinkKakao(Long userId) {
        User user = getActiveUser(userId);

        UserOAuthProvider oauthLink = oAuthProviderRepository.findByUserAndProvider(user, OAuthProvider.KAKAO)
                .orElseThrow(() -> new AuthException("연동된 카카오 계정이 없습니다.", HttpStatus.BAD_REQUEST));

        if (!user.isLocal() && oAuthProviderRepository.countByUser(user) <= 1) {
            throw new AuthException("마지막 로그인 수단은 해제할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }

        oAuthProviderRepository.delete(oauthLink);
    }

    // ── 공통 사용자 처리 ─────────────────────────────────────────────────────

    private OAuthLoginResponse findOrCreateUser(OAuthProvider provider, String providerId,
                                                String email, String name) {
        // 동일 이메일 계정 존재 시 자동 연동 금지 — 명시적 연동 엔드포인트 사용 안내
        if (email != null && userRepository.findByEmail(email).isPresent()) {
            throw new AuthException(
                    "동일한 이메일로 이미 가입된 계정이 있습니다. 기존 계정으로 로그인한 후 소셜 계정 연동 기능을 이용해 주세요.",
                    HttpStatus.CONFLICT);
        }

        // 신규 계정 생성
        User newUser = User.builder()
                .uuid(UUID.randomUUID().toString())
                .email(email)
                .build();
        userRepository.save(newUser);
        profileRepository.save(Profile.builder()
                .user(newUser)
                .nickname(generateUniqueNickname(name))
                .build());
        oAuthProviderRepository.save(UserOAuthProvider.of(newUser, provider, providerId));
        return issueTokens(newUser, true);
    }

    private OAuthLoginResponse issueTokens(User user, boolean isNew) {
        user.updateLastLoginAt(LocalDateTime.now());

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getRole().name());
        String refreshTokenStr = jwtTokenProvider.generateRefreshToken(user.getId());

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(jwtTokenProvider.hashToken(refreshTokenStr))
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build());

        long expiresIn = jwtProperties.getAccessTokenExpiry() / 1000;
        String nickname = profileRepository.findByUser(user)
                .map(Profile::getNickname)
                .orElse(null);
        return OAuthLoginResponse.of(accessToken, refreshTokenStr, expiresIn, isNew, user, nickname);
    }

    private User getActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND));
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new AuthException("이미 탈퇴한 계정입니다.", HttpStatus.GONE);
        }
        return user;
    }

    private String generateUniqueNickname(String base) {
        String candidate = base;
        int suffix = 1;
        while (profileRepository.existsByNickname(candidate)) {
            candidate = base + suffix++;
        }
        return candidate;
    }

    // ── Google 내부 ──────────────────────────────────────────────────────────

    private String exchangeGoogleCode(String code, String redirectUri) {
        OAuthProperties.Provider cfg = oAuthProperties.getGoogle();
        String body = "code=" + encode(code)
                + "&client_id=" + encode(cfg.getClientId())
                + "&client_secret=" + encode(cfg.getClientSecret())
                + "&redirect_uri=" + encode(redirectUri)
                + "&grant_type=authorization_code";

        JsonNode json = postForm("https://oauth2.googleapis.com/token", body);
        JsonNode tokenNode = json.get("access_token");
        if (tokenNode == null || tokenNode.isNull()) {
            // 내부 에러 서버 로그에만 기록, 클라이언트에는 일반 메시지 반환
            log.error("[구글 토큰 교환 실패] error={}, description={}", json.path("error").asText(), json.path("error_description").asText());
            throw new AuthException("Google OAuth 인증에 실패했습니다. 인가 코드를 다시 확인해주세요.", HttpStatus.BAD_REQUEST);
        }
        log.debug("[구글 토큰 교환 성공]");
        return tokenNode.asText();
    }

    private JsonNode getGoogleUserInfo(String accessToken) {
        return getWithBearer("https://www.googleapis.com/oauth2/v3/userinfo", accessToken);
    }

    // ── Kakao 내부 ───────────────────────────────────────────────────────────

    private String exchangeKakaoCode(String code, String redirectUri) {
        OAuthProperties.Provider cfg = oAuthProperties.getKakao();
        String body = "grant_type=authorization_code"
                + "&client_id=" + encode(cfg.getClientId())
                + "&client_secret=" + encode(cfg.getClientSecret())
                + "&redirect_uri=" + encode(redirectUri)
                + "&code=" + encode(code);

        JsonNode json = postForm("https://kauth.kakao.com/oauth/token", body);
        JsonNode tokenNode = json.get("access_token");
        if (tokenNode == null || tokenNode.isNull()) {
            // 내부 에러 서버 로그에만 기록, 클라이언트에는 일반 메시지 반환
            log.error("[카카오 토큰 교환 실패] error={}, description={}", json.path("error").asText(), json.path("error_description").asText());
            throw new AuthException("Kakao OAuth 인증에 실패했습니다. 인가 코드를 다시 확인해주세요.", HttpStatus.BAD_REQUEST);
        }
        log.debug("[카카오 토큰 교환 성공]");
        return tokenNode.asText();
    }

    private JsonNode getKakaoUserInfo(String accessToken) {
        return getWithBearer("https://kapi.kakao.com/v2/user/me", accessToken);
    }

    // ── redirect_uri 검증 ────────────────────────────────────────────────────

    private String resolveAndValidateRedirectUri(String requestedUri, OAuthProperties.Provider cfg) {
        // 공백·빈 문자열 → null 정규화
        String normalized = (requestedUri != null && !requestedUri.isBlank()) ? requestedUri : null;
        if (normalized == null) {
            return cfg.getRedirectUri();
        }
        // 허용 목록 검사 (목록이 비어 있으면 기본 redirectUri만 허용)
        List<String> allowlist = cfg.getAllowedRedirectUris();
        boolean permitted = (allowlist != null && !allowlist.isEmpty())
                ? allowlist.contains(normalized)
                : normalized.equals(cfg.getRedirectUri());
        if (!permitted) {
            throw new AuthException("허용되지 않은 redirect_uri 입니다.", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    // ── HTTP 유틸 ────────────────────────────────────────────────────────────

    private JsonNode postForm(String url, String formBody) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());

            if (response.statusCode() >= 400) {
                String error = json.path("error_description").asText(json.path("error").asText("OAuth 요청 실패"));
                throw new AuthException(error, HttpStatus.valueOf(response.statusCode()));
            }
            return json;
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException("OAuth 서버와 통신 중 오류가 발생했습니다: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private JsonNode getWithBearer(String url, String accessToken) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());

            if (response.statusCode() >= 400) {
                // 내부 에러 상세는 서버 로그에만 기록
                log.error("[OAuth 사용자 정보 조회 실패] url={}, status={}", url, response.statusCode());
                throw new AuthException("OAuth 사용자 정보를 가져오는 데 실패했습니다. 다시 시도해주세요.", HttpStatus.valueOf(response.statusCode()));
            }
            return json;
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException("사용자 정보 조회 중 오류가 발생했습니다: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value != null ? value : "", StandardCharsets.UTF_8);
    }
}
