package com.eodigaljido.backend.security;

import com.eodigaljido.backend.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StompJwtChannelInterceptorTest {

    private JwtTokenProvider jwtTokenProvider;
    private StompJwtChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-that-is-at-least-thirty-two-bytes-long");
        properties.setAccessTokenExpiry(60_000);
        properties.setRefreshTokenExpiry(60_000);
        jwtTokenProvider = new JwtTokenProvider(properties);
        interceptor = new StompJwtChannelInterceptor(jwtTokenProvider);
    }

    @Test
    void authenticatesConnectFrameWithAccessToken() {
        Message<byte[]> message = connectMessage(jwtTokenProvider.generateAccessToken(7L, "USER"));

        Message<?> result = interceptor.preSend(message, null);

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
        assertThat(accessor.getUser()).isNotNull();
        assertThat(accessor.getUser().getName()).isEqualTo("7");
    }

    @Test
    void rejectsRefreshTokenOnConnect() {
        Message<byte[]> message = connectMessage(jwtTokenProvider.generateRefreshToken(7L));

        assertThatThrownBy(() -> interceptor.preSend(message, null))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("WebSocket authentication failed");
    }

    @Test
    void leavesSendFramesForMessageMappingHandler() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/chat/room-uuid/typing");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThat(interceptor.preSend(message, null)).isSameAs(message);
    }

    private Message<byte[]> connectMessage(String token) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
