package com.eodigaljido.backend.security;

import com.eodigaljido.backend.exception.ChatException;
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

        String token = resolveToken(accessor.getFirstNativeHeader("Authorization"));
        if (!StringUtils.hasText(token) || !jwtTokenProvider.isValid(token)) {
            log.warn("[STOMP] JWT 인증 실패 - 연결 거부");
            throw new ChatException("WebSocket 인증에 실패했습니다.", HttpStatus.UNAUTHORIZED);
        }

        Long userId;
        try {
            userId = jwtTokenProvider.getUserId(token);
        } catch (NumberFormatException e) {
            log.warn("[STOMP] JWT subject 파싱 실패 - 연결 거부");
            throw new ChatException("WebSocket 인증에 실패했습니다.", HttpStatus.UNAUTHORIZED);
        }
        accessor.setUser(() -> String.valueOf(userId));
        log.debug("[STOMP] 인증 성공 - userId={}", userId);
        return message;
    }

    private String resolveToken(String authHeader) {
        if (!StringUtils.hasText(authHeader)) return null;
        String[] parts = authHeader.trim().split("\\s+");
        if (parts.length == 2 && parts[0].equalsIgnoreCase("Bearer")) {
            return parts[1];
        }
        return null;
    }
}
