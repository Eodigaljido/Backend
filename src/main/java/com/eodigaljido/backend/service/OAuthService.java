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
import java.util.Optional;
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

    // ── Google ────────────────────────────────────────────────────────────────

    @Transactional
    public OAuthLoginResponse loginWithGoogle(String authorizationCode, String redirectUri) {
        String effectiveRedirectUri = redirectUri != null ? redirectUri : oAuthProperties.getGoogle().getRedirectUri();
        String accessToken = exchangeGoogleCode(authorizationCode, effectiveRedirectUri);
        JsonNode userInfo = getGoogleUserInfo(accessToken);

        String providerId = userInfo.get("sub").asText();
        String email = userInfo.path("email").asText(null);
        String name = userInfo.path("name").asText("Google User");

        return oAuthProviderRepository.findByProviderAndProviderId(OAuthProvider.GOOGLE, providerId)
                .map(oap -> issueTokens(oap.getUser(), false))
                .orElseGet(() -> findOrCreateUser(OAuthProvider.GOOGLE, providerId, email, name));
    }

    // ── Kakao ────────────────────────────────────────────────────────────────

    @Transactional
    public OAuthLoginResponse loginWithKakao(String authorizationCode, String redirectUri) {
        String effectiveRedirectUri = redirectUri != null ? redirectUri : oAuthProperties.getKakao().getRedirectUri();
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

    // ── 구글 연동 ──────────────────────────────────────────────────────────────

    @Transactional
    public void linkGoogle(Long userId, String authorizationCode, String redirectUri) {
        User user = getActiveUser(userId);

        if (oAuthProviderRepository.existsByUserAndProvider(user, OAuthProvider.GOOGLE)) {
            throw new AuthException("이미 구글 계정이 연동되어 있습니다.", HttpStatus.CONFLICT);
        }

        String effectiveRedirectUri = redirectUri != null ? redirectUri : oAuthProperties.getGoogle().getRedirectUri();
        String googleAccessToken = exchangeGoogleCode(authorizationCode, effectiveRedirectUri);
        String googleId = getGoogleUserInfo(googleAccessToken).get("sub").asText();

        if (oAuthProviderRepository.existsByProviderAndProviderId(OAuthProvider.GOOGLE, googleId)) {
            throw new AuthException("해당 구글 계정은 이미 다른 계정에 연결되어 있습니다.", HttpStatus.CONFLICT);
        }

        oAuthProviderRepository.save(UserOAuthProvider.of(user, OAuthProvider.GOOGLE, googleId));
    }

    // ── 구글 연동 해제 ─────────────────────────────────────────────────────────

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

    // ── 카카오 연동 ──────────────────────────────────────────────────────────

    @Transactional
    public void linkKakao(Long userId, String authorizationCode, String redirectUri) {
        User user = getActiveUser(userId);

        if (oAuthProviderRepository.existsByUserAndProvider(user, OAuthProvider.KAKAO)) {
            throw new AuthException("이미 카카오 계정이 연동되어 있습니다.", HttpStatus.CONFLICT);
        }

        String effectiveRedirectUri = redirectUri != null ? redirectUri : oAuthProperties.getKakao().getRedirectUri();
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
        // 동일 이메일 계정이 있으면 자동 연동 후 로그인
        if (email != null) {
            Optional<User> byEmail = userRepository.findByEmail(email);
            if (byEmail.isPresent()) {
                User existing = byEmail.get();
                if (!oAuthProviderRepository.existsByUserAndProvider(existing, provider)) {
                    oAuthProviderRepository.save(UserOAuthProvider.of(existing, provider, providerId));
                    log.info("[OAuth 자동 연동] provider={}, email={}", provider, email);
                }
                return issueTokens(existing, false);
            }
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
            String error = json.path("error_description").asText(json.path("error").asText("access_token 없음"));
            log.error("[구글 토큰 교환 실패] 응답: {}", json);
            throw new AuthException("구글 토큰 발급 실패: " + error, HttpStatus.BAD_REQUEST);
        }
        log.debug("[구글 토큰 교환 성공] redirect_uri={}", redirectUri);
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
            String error = json.path("error_description").asText(json.path("error").asText("access_token 없음"));
            log.error("[카카오 토큰 교환 실패] 응답: {}", json);
            throw new AuthException("카카오 토큰 발급 실패: " + error, HttpStatus.BAD_REQUEST);
        }
        log.debug("[카카오 토큰 교환 성공] redirect_uri={}", redirectUri);
        return tokenNode.asText();
    }

    private JsonNode getKakaoUserInfo(String accessToken) {
        return getWithBearer("https://kapi.kakao.com/v2/user/me", accessToken);
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
                String errMsg = json.path("msg").asText(
                        json.path("error_description").asText(
                                json.path("error").asText("사용자 정보 조회 실패")));
                log.error("[OAuth 사용자 정보 조회 실패] url={}, status={}, body={}", url, response.statusCode(), json);
                throw new AuthException("OAuth 사용자 정보 조회 실패: " + errMsg, HttpStatus.valueOf(response.statusCode()));
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
