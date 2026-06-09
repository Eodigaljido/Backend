package com.eodigaljido.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.PrematureJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private static final Duration FAILURE_LOG_INTERVAL = Duration.ofSeconds(60);

    private final JwtTokenProvider jwtTokenProvider;
    private final ConcurrentMap<String, SuppressedFailure> suppressedFailures = new ConcurrentHashMap<>();

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
            throw new MessageDeliveryException(message, "WebSocket 인증에 실패했습니다.");
        }

        Claims claims;
        try {
            claims = jwtTokenProvider.parseToken(tokenResolution.token());
        } catch (ExpiredJwtException e) {
            logAuthFailure(accessor, "token_expired", tokenResolution.token(),
                    "expiredAt=" + formatDate(e.getClaims() != null ? e.getClaims().getExpiration() : null));
            throw new MessageDeliveryException(message, "WebSocket 인증에 실패했습니다.");
        } catch (PrematureJwtException e) {
            logAuthFailure(accessor, "token_not_yet_valid", tokenResolution.token(), e.getMessage());
            throw new MessageDeliveryException(message, "WebSocket 인증에 실패했습니다.");
        } catch (MalformedJwtException e) {
            logAuthFailure(accessor, "token_malformed", tokenResolution.token(), e.getMessage());
            throw new MessageDeliveryException(message, "WebSocket 인증에 실패했습니다.");
        } catch (UnsupportedJwtException e) {
            logAuthFailure(accessor, "token_unsupported", tokenResolution.token(), e.getMessage());
            throw new MessageDeliveryException(message, "WebSocket 인증에 실패했습니다.");
        } catch (io.jsonwebtoken.security.SecurityException e) {
            logAuthFailure(accessor, "token_signature_invalid", tokenResolution.token(), e.getMessage());
            throw new MessageDeliveryException(message, "WebSocket 인증에 실패했습니다.");
        } catch (JwtException | IllegalArgumentException e) {
            logAuthFailure(accessor, "token_invalid", tokenResolution.token(), e.getMessage());
            throw new MessageDeliveryException(message, "WebSocket 인증에 실패했습니다.");
        }

        String subject = claims.getSubject();
        if (!StringUtils.hasText(subject)) {
            logAuthFailure(accessor, "subject_missing", tokenResolution.token(), null);
            throw new MessageDeliveryException(message, "WebSocket 인증에 실패했습니다.");
        }

        Long userId;
        try {
            userId = Long.parseLong(subject);
        } catch (NumberFormatException e) {
            logAuthFailure(accessor, "subject_not_numeric", tokenResolution.token(),
                    "subjectLength=" + subject.length());
            throw new MessageDeliveryException(message, "WebSocket 인증에 실패했습니다.");
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
        String tokenHash = tokenHashPrefix(token);
        String suppressionKey = reason + ":" + tokenHash;
        SuppressedFailure suppressedFailure = suppressedFailures.computeIfAbsent(
                suppressionKey, ignored -> new SuppressedFailure());
        int suppressedCount = suppressedFailure.beforeLogIfDue();
        if (suppressedCount < 0) {
            return;
        }

        log.warn("[STOMP] JWT 인증 실패 - 연결 거부 reason={} sessionId={} acceptVersion={} heartbeat={} tokenHash={} detail={}",
                reason,
                accessor.getSessionId(),
                accessor.getAcceptVersion(),
                accessor.getHeartbeat(),
                tokenHash,
                appendSuppressedCount(sanitizeDetail(detail), suppressedCount));
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

    private String appendSuppressedCount(String detail, int suppressedCount) {
        if (suppressedCount <= 0) {
            return detail;
        }
        String suffix = "suppressedSinceLast=" + suppressedCount;
        return StringUtils.hasText(detail) ? detail + ", " + suffix : suffix;
    }

    private record TokenResolution(String token, String reason) {
    }

    private static class SuppressedFailure {

        private Instant lastLoggedAt = Instant.EPOCH;
        private final AtomicInteger suppressedCount = new AtomicInteger();

        synchronized int beforeLogIfDue() {
            Instant now = Instant.now();
            if (Duration.between(lastLoggedAt, now).compareTo(FAILURE_LOG_INTERVAL) < 0) {
                suppressedCount.incrementAndGet();
                return -1;
            }

            lastLoggedAt = now;
            return suppressedCount.getAndSet(0);
        }
    }
}
