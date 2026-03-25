package com.eodigaljido.backend.controller;

import com.eodigaljido.backend.dto.chat.SendMessageRequest;
import com.eodigaljido.backend.exception.ChatException;
import com.eodigaljido.backend.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

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
        Long userId = Long.valueOf(principal.getName());
        chatService.sendMessage(userId, roomUuid, req);
    }
}
