package com.eodigaljido.backend.service;

import com.eodigaljido.backend.config.JwtProperties;
import com.eodigaljido.backend.config.OAuthProperties;
import com.eodigaljido.backend.domain.user.Profile;
import com.eodigaljido.backend.domain.user.RefreshToken;
import com.eodigaljido.backend.domain.user.User;
import com.eodigaljido.backend.dto.auth.OAuthLoginResponse;
import com.eodigaljido.backend.exception.AuthException;
import com.eodigaljido.backend.repository.ProfileRepository;
import com.eodigaljido.backend.repository.RefreshTokenRepository;
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

        // 구글 전용 계정 먼저 검색, 없으면 연동된 LOCAL 계정 검색
        return userRepository.findByProviderAndProviderId(User.Provider.GOOGLE, providerId)
                .map(user -> issueTokens(user, false))
                .orElseGet(() -> userRepository.findByLinkedGoogleId(providerId)
                        .map(user -> issueTokens(user, false))
                        .orElseGet(() -> findOrCreateUser(User.Provider.GOOGLE, providerId, email, name)));
    }

    // ── 구글 연동 ──────────────────────────────────────────────────────────────

    @Transactional
    public void linkGoogle(Long userId, String authorizationCode, String redirectUri) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND));

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new AuthException("이미 탈퇴한 계정입니다.", HttpStatus.GONE);
        }
        if (user.getLinkedGoogleId() != null) {
            throw new AuthException("이미 구글 계정이 연동되어 있습니다.", HttpStatus.CONFLICT);
        }
        if (user.getProvider() == User.Provider.GOOGLE) {
            throw new AuthException("구글로 가입한 계정은 연동이 필요하지 않습니다.", HttpStatus.BAD_REQUEST);
        }

        String effectiveRedirectUri = redirectUri != null ? redirectUri : oAuthProperties.getGoogle().getRedirectUri();
        String googleAccessToken = exchangeGoogleCode(authorizationCode, effectiveRedirectUri);
        JsonNode userInfo = getGoogleUserInfo(googleAccessToken);
        String googleId = userInfo.get("sub").asText();

        if (userRepository.existsByProviderAndProviderId(User.Provider.GOOGLE, googleId)) {
            throw new AuthException("해당 구글 계정은 이미 다른 계정에 연결되어 있습니다.", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByLinkedGoogleId(googleId)) {
            throw new AuthException("해당 구글 계정은 이미 다른 계정에 연결되어 있습니다.", HttpStatus.CONFLICT);
        }

        user.linkGoogle(googleId);
    }

    // ── 구글 연동 해제 ─────────────────────────────────────────────────────────

    @Transactional
    public void unlinkGoogle(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND));

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new AuthException("이미 탈퇴한 계정입니다.", HttpStatus.GONE);
        }
        if (user.getProvider() == User.Provider.GOOGLE) {
            throw new AuthException("구글로 가입한 계정은 연동 해제가 불가능합니다.", HttpStatus.BAD_REQUEST);
        }
        if (user.getLinkedGoogleId() == null) {
            throw new AuthException("연동된 구글 계정이 없습니다.", HttpStatus.BAD_REQUEST);
        }

        user.unlinkGoogle();
    }

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

        // 카카오 전용 계정 먼저 검색, 없으면 연동된 LOCAL 계정 검색
        return userRepository.findByProviderAndProviderId(User.Provider.KAKAO, providerId)
                .map(user -> issueTokens(user, false))
                .orElseGet(() -> userRepository.findByLinkedKakaoId(providerId)
                        .map(user -> issueTokens(user, false))
                        .orElseGet(() -> findOrCreateUser(User.Provider.KAKAO, providerId, email, name)));
    }

    // ── 카카오 연동 ──────────────────────────────────────────────────────────

    @Transactional
    public void linkKakao(Long userId, String authorizationCode, String redirectUri) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND));

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new AuthException("이미 탈퇴한 계정입니다.", HttpStatus.GONE);
        }
        if (user.getLinkedKakaoId() != null) {
            throw new AuthException("이미 카카오 계정이 연동되어 있습니다.", HttpStatus.CONFLICT);
        }
        if (user.getProvider() == User.Provider.KAKAO) {
            throw new AuthException("카카오로 가입한 계정은 연동이 필요하지 않습니다.", HttpStatus.BAD_REQUEST);
        }

        String effectiveRedirectUri = redirectUri != null ? redirectUri : oAuthProperties.getKakao().getRedirectUri();
        String kakaoAccessToken = exchangeKakaoCode(authorizationCode, effectiveRedirectUri);
        JsonNode userInfo = getKakaoUserInfo(kakaoAccessToken);
        String kakaoId = userInfo.get("id").asText();

        if (userRepository.existsByProviderAndProviderId(User.Provider.KAKAO, kakaoId)) {
            throw new AuthException("해당 카카오 계정은 이미 다른 계정에 연결되어 있습니다.", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByLinkedKakaoId(kakaoId)) {
            throw new AuthException("해당 카카오 계정은 이미 다른 계정에 연결되어 있습니다.", HttpStatus.CONFLICT);
        }

        user.linkKakao(kakaoId);
    }

    // ── 카카오 연동 해제 ─────────────────────────────────────────────────────

    @Transactional
    public void unlinkKakao(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND));

        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new AuthException("이미 탈퇴한 계정입니다.", HttpStatus.GONE);
        }
        if (user.getProvider() == User.Provider.KAKAO) {
            throw new AuthException("카카오로 가입한 계정은 연동 해제가 불가능합니다.", HttpStatus.BAD_REQUEST);
        }
        if (user.getLinkedKakaoId() == null) {
            throw new AuthException("연동된 카카오 계정이 없습니다.", HttpStatus.BAD_REQUEST);
        }

        user.unlinkKakao();
    }

    // ── 공통 사용자 처리 ─────────────────────────────────────────────────────

    private OAuthLoginResponse findOrCreateUser(User.Provider provider, String providerId,
                                                String email, String name) {
        boolean[] isNew = {false};
        User user = userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> {
                    // 동일 이메일 계정이 있으면 자동 연동 후 로그인
                    if (email != null) {
                        Optional<User> byEmail = userRepository.findByEmail(email);
                        if (byEmail.isPresent()) {
                            User existing = byEmail.get();
                            if (provider == User.Provider.GOOGLE && existing.getLinkedGoogleId() == null) {
                                existing.linkGoogle(providerId);
                                log.info("[OAuth 자동 연동] provider=GOOGLE, email={}", email);
                            } else if (provider == User.Provider.KAKAO && existing.getLinkedKakaoId() == null) {
                                existing.linkKakao(providerId);
                                log.info("[OAuth 자동 연동] provider=KAKAO, email={}", email);
                            }
                            return existing;
                        }
                    }
                    isNew[0] = true;
                    return createOAuthUser(provider, providerId, email, name);
                });

        return issueTokens(user, isNew[0]);
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
                // 카카오는 {"msg": "...", "code": -401}, 구글은 {"error": "...", "error_description": "..."} 형식
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
