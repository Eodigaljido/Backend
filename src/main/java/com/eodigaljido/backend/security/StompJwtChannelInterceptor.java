package com.eodigaljido.backend.security;

import com.eodigaljido.backend.exception.ChatException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.PrematureJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        TokenResolution tokenResolution = resolveToken(accessor.getFirstNativeHeader("Authorization"));
        if (!StringUtils.hasText(tokenResolution.token())) {
            logAuthFailure(accessor, tokenResolution.reason(), null, null);
            throw new ChatException("WebSocket 인증에 실패했습니다.", HttpStatus.UNAUTHORIZED);
        }

        Claims claims;
        try {
            claims = jwtTokenProvider.parseToken(tokenResolution.token());
        } catch (ExpiredJwtException e) {
            logAuthFailure(accessor, "token_expired", tokenResolution.token(),
                    "expiredAt=" + formatDate(e.getClaims() != null ? e.getClaims().getExpiration() : null));
            throw new ChatException("WebSocket 인증에 실패했습니다.", HttpStatus.UNAUTHORIZED);
        } catch (PrematureJwtException e) {
            logAuthFailure(accessor, "token_not_yet_valid", tokenResolution.token(), e.getMessage());
            throw new ChatException("WebSocket 인증에 실패했습니다.", HttpStatus.UNAUTHORIZED);
        } catch (MalformedJwtException e) {
            logAuthFailure(accessor, "token_malformed", tokenResolution.token(), e.getMessage());
            throw new ChatException("WebSocket 인증에 실패했습니다.", HttpStatus.UNAUTHORIZED);
        } catch (UnsupportedJwtException e) {
            logAuthFailure(accessor, "token_unsupported", tokenResolution.token(), e.getMessage());
            throw new ChatException("WebSocket 인증에 실패했습니다.", HttpStatus.UNAUTHORIZED);
        } catch (io.jsonwebtoken.security.SecurityException e) {
            logAuthFailure(accessor, "token_signature_invalid", tokenResolution.token(), e.getMessage());
            throw new ChatException("WebSocket 인증에 실패했습니다.", HttpStatus.UNAUTHORIZED);
        } catch (JwtException | IllegalArgumentException e) {
            logAuthFailure(accessor, "token_invalid", tokenResolution.token(), e.getMessage());
            throw new ChatException("WebSocket 인증에 실패했습니다.", HttpStatus.UNAUTHORIZED);
        }

        String subject = claims.getSubject();
        if (!StringUtils.hasText(subject)) {
            logAuthFailure(accessor, "subject_missing", tokenResolution.token(), null);
            throw new ChatException("WebSocket 인증에 실패했습니다.", HttpStatus.UNAUTHORIZED);
        }

        Long userId;
        try {
            userId = Long.parseLong(subject);
        } catch (NumberFormatException e) {
            logAuthFailure(accessor, "subject_not_numeric", tokenResolution.token(),
                    "subjectLength=" + subject.length());
            throw new ChatException("WebSocket 인증에 실패했습니다.", HttpStatus.UNAUTHORIZED);
        }

        accessor.setUser(() -> String.valueOf(userId));
        log.debug("[STOMP] 인증 성공 - userId={}", userId);
        return message;
    }

    private TokenResolution resolveToken(String authHeader) {
        if (authHeader == null) {
            return new TokenResolution(null, "authorization_header_missing");
        }
        if (!StringUtils.hasText(authHeader)) {
            return new TokenResolution(null, "authorization_header_blank");
        }
        String[] parts = authHeader.trim().split("\\s+");
        if (parts.length == 2 && parts[0].equalsIgnoreCase("Bearer")) {
            return new TokenResolution(parts[1], null);
        }
        if (parts.length == 1 && parts[0].equalsIgnoreCase("Bearer")) {
            return new TokenResolution(null, "bearer_token_missing");
        }
        return new TokenResolution(null, "authorization_header_malformed");
    }

    private void logAuthFailure(StompHeaderAccessor accessor, String reason, String token, String detail) {
        log.warn("[STOMP] JWT 인증 실패 - 연결 거부 reason={} sessionId={} acceptVersion={} heartbeat={} tokenHash={} detail={}",
                reason,
                accessor.getSessionId(),
                accessor.getAcceptVersion(),
                accessor.getHeartbeat(),
                tokenHashPrefix(token),
                sanitizeDetail(detail));
    }

    private String tokenHashPrefix(String token) {
        if (!StringUtils.hasText(token)) {
            return "none";
        }
        String hash = jwtTokenProvider.hashToken(token);
        return hash.length() > 12 ? hash.substring(0, 12) : hash;
    }

    private String formatDate(Date date) {
        return date != null ? date.toInstant().toString() : "unknown";
    }

    private String sanitizeDetail(String detail) {
        if (!StringUtils.hasText(detail)) {
            return null;
        }
        String sanitized = detail.replaceAll("[\\r\\n\\t]+", " ");
        return sanitized.length() > 160 ? sanitized.substring(0, 160) + "..." : sanitized;
    }

    private record TokenResolution(String token, String reason) {
    }
}
