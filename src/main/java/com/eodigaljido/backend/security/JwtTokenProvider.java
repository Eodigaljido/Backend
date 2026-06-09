package com.eodigaljido.backend.security;

import com.eodigaljido.backend.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;

    private static final int MIN_SECRET_BYTES = 32; // HS256 최소 256비트(32바이트)

    private SecretKey getSigningKey() {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET 환경변수가 설정되지 않았습니다.");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET은 최소 " + MIN_SECRET_BYTES + "바이트(256비트) 이상이어야 합니다. 현재: " + keyBytes.length + "바이트");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(Long userId, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .claim("typ", "access")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtProperties.getAccessTokenExpiry()))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("typ", "refresh")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtProperties.getRefreshTokenExpiry()))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isValidAccessToken(String token) {
        return validateAccessToken(token).status() == AccessTokenStatus.VALID;
    }

    public boolean isValidRefreshToken(String token) {
        try {
            return "refresh".equals(parseToken(token).get("typ", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public AccessTokenValidation validateAccessToken(String token) {
        if (token == null || token.isBlank()) {
            return new AccessTokenValidation(AccessTokenStatus.MISSING, null);
        }
        try {
            Claims claims = parseToken(token);
            if (!"access".equals(claims.get("typ", String.class))) {
                return new AccessTokenValidation(AccessTokenStatus.WRONG_TYPE, null);
            }
            if (claims.getSubject() == null) {
                return new AccessTokenValidation(AccessTokenStatus.INVALID, null);
            }
            Long.parseLong(claims.getSubject());
            return new AccessTokenValidation(AccessTokenStatus.VALID, claims);
        } catch (ExpiredJwtException e) {
            return new AccessTokenValidation(AccessTokenStatus.EXPIRED, null);
        } catch (JwtException | IllegalArgumentException e) {
            return new AccessTokenValidation(AccessTokenStatus.INVALID, null);
        }
    }

    public Long getUserId(String token) {
        return Long.parseLong(parseToken(token).getSubject());
    }

    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("토큰 해싱 실패", e);
        }
    }

    public enum AccessTokenStatus {
        VALID,
        MISSING,
        EXPIRED,
        INVALID,
        WRONG_TYPE,
        USER_INACTIVE
    }

    public record AccessTokenValidation(AccessTokenStatus status, Claims claims) {
    }
}
