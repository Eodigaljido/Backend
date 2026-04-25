package com.eodigaljido.backend.controller;

import com.eodigaljido.backend.dto.chat.SendMessageRequest;
import com.eodigaljido.backend.dto.chat.TypingRequest;
import com.eodigaljido.backend.dto.common.ErrorResponse;
import com.eodigaljido.backend.exception.ChatException;
import com.eodigaljido.backend.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;

    @MessageMapping("/chat/{roomUuid}")
    public void handleMessage(
            @DestinationVariable String roomUuid,
            @Valid SendMessageRequest req,
            Principal principal) {
        if (principal == null) {
            throw new ChatException("인증이 필요합니다.", HttpStatus.UNAUTHORIZED);
        }
        Long userId;
        try {
            userId = Long.valueOf(principal.getName());
        } catch (NumberFormatException e) {
            throw new ChatException("유효하지 않은 사용자 인증 정보입니다.", HttpStatus.UNAUTHORIZED);
        }
        chatService.sendMessage(userId, roomUuid, req);
    }

    @MessageMapping("/chat/{roomUuid}/typing")
    public void handleTyping(
            @DestinationVariable String roomUuid,
            TypingRequest req,
            Principal principal) {
        if (principal == null) {
            throw new ChatException("인증이 필요합니다.", HttpStatus.UNAUTHORIZED);
        }
        Long userId;
        try {
            userId = Long.valueOf(principal.getName());
        } catch (NumberFormatException e) {
            throw new ChatException("유효하지 않은 사용자 인증 정보입니다.", HttpStatus.UNAUTHORIZED);
        }
        chatService.broadcastTyping(userId, roomUuid, req);
    }

    @MessageExceptionHandler(ChatException.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleChatException(ChatException e) {
        log.warn("[WebSocket 채팅 오류] {}", e.getMessage());
        return new ErrorResponse(e.getStatus().value(), e.getMessage(), LocalDateTime.now());
    }

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + (fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "유효하지 않은 값"))
                .collect(Collectors.joining(", "));
        if (message.isBlank()) message = "유효하지 않은 요청입니다.";
        log.warn("[WebSocket 유효성 오류] {}", message);
        return new ErrorResponse(400, message, LocalDateTime.now());
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleException(Exception e) {
        log.error("[WebSocket 서버 오류] {}", e.getMessage(), e);
        return new ErrorResponse(500, "서버 오류가 발생했습니다.", LocalDateTime.now());
    }
}
