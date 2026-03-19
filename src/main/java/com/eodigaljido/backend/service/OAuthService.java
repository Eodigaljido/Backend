package com.eodigaljido.backend.service;

import com.eodigaljido.backend.config.JwtProperties;
import com.eodigaljido.backend.config.OAuthProperties;
import com.eodigaljido.backend.domain.user.Profile;
import com.eodigaljido.backend.domain.user.RefreshToken;
import com.eodigaljido.backend.domain.user.User;
import com.eodigaljido.backend.dto.auth.LoginResponse;
import com.eodigaljido.backend.dto.auth.OAuthLoginResponse;
import com.eodigaljido.backend.exception.AuthException;
import com.eodigaljido.backend.repository.ProfileRepository;
import com.eodigaljido.backend.repository.RefreshTokenRepository;
import com.eodigaljido.backend.repository.UserRepository;
import com.eodigaljido.backend.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuthService {

    private final OAuthProperties oAuthProperties;
    private final JwtProperties jwtProperties;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── Google ────────────────────────────────────────────────────────────────

    @Transactional
    public OAuthLoginResponse loginWithGoogle(String authorizationCode) {
        String accessToken = exchangeGoogleCode(authorizationCode);
        JsonNode userInfo = getGoogleUserInfo(accessToken);

        String providerId = userInfo.get("sub").asText();
        String email = userInfo.path("email").asText(null);
        String name = userInfo.path("name").asText("Google User");

        return findOrCreateUser(User.Provider.GOOGLE, providerId, email, name);
    }

    private String exchangeGoogleCode(String code) {
        OAuthProperties.Provider cfg = oAuthProperties.getGoogle();
        String body = "code=" + encode(code)
                + "&client_id=" + encode(cfg.getClientId())
                + "&client_secret=" + encode(cfg.getClientSecret())
                + "&redirect_uri=" + encode(cfg.getRedirectUri())
                + "&grant_type=authorization_code";

        JsonNode json = postForm("https://oauth2.googleapis.com/token", body);
        return json.get("access_token").asText();
    }

    private JsonNode getGoogleUserInfo(String accessToken) {
        return getWithBearer("https://www.googleapis.com/oauth2/v3/userinfo", accessToken);
    }

    // ── Kakao ────────────────────────────────────────────────────────────────

    @Transactional
    public OAuthLoginResponse loginWithKakao(String authorizationCode) {
        String accessToken = exchangeKakaoCode(authorizationCode);
        JsonNode userInfo = getKakaoUserInfo(accessToken);

        String providerId = userInfo.get("id").asText();
        JsonNode account = userInfo.path("kakao_account");
        String email = account.path("email").asText(null);
        String name = account.path("profile").path("nickname").asText("Kakao User");

        return findOrCreateUser(User.Provider.KAKAO, providerId, email, name);
    }

    private String exchangeKakaoCode(String code) {
        OAuthProperties.Provider cfg = oAuthProperties.getKakao();
        String body = "grant_type=authorization_code"
                + "&client_id=" + encode(cfg.getClientId())
                + "&client_secret=" + encode(cfg.getClientSecret())
                + "&redirect_uri=" + encode(cfg.getRedirectUri())
                + "&code=" + encode(code);

        JsonNode json = postForm("https://kauth.kakao.com/oauth/token", body);
        return json.get("access_token").asText();
    }

    private JsonNode getKakaoUserInfo(String accessToken) {
        return getWithBearer("https://kapi.kakao.com/v2/user/me", accessToken);
    }

    // ── 공통 사용자 처리 ─────────────────────────────────────────────────────

    private OAuthLoginResponse findOrCreateUser(User.Provider provider, String providerId,
                                                String email, String name) {
        boolean[] isNew = {false};
        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> {
                    isNew[0] = true;
                    return createOAuthUser(provider, providerId, email, name);
                });

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
        return OAuthLoginResponse.of(accessToken, refreshTokenStr, expiresIn, isNew[0], user, nickname);
    }

    private User createOAuthUser(User.Provider provider, String providerId,
                                 String email, String name) {
        User user = User.builder()
                .uuid(UUID.randomUUID().toString())
                .email(email)
                .provider(provider)
                .providerId(providerId)
                .build();

        userRepository.save(user);

        profileRepository.save(Profile.builder()
                .user(user)
                .nickname(generateUniqueNickname(name))
                .build());

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
                throw new AuthException("사용자 정보 조회 실패", HttpStatus.valueOf(response.statusCode()));
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
