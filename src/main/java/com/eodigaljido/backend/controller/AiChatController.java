package com.eodigaljido.backend.controller;

import com.eodigaljido.backend.dto.ai.*;
import com.eodigaljido.backend.service.AiChatService;
import com.eodigaljido.backend.service.ChatSessionService;
import com.eodigaljido.backend.service.GroupRouteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Tag(name = "AI Chat", description = "루티 AI 챗봇 — 상황 기반 재설계 및 경로 편집 보조")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;
    private final GroupRouteService groupRouteService;
    private final ChatSessionService sessionService;

    @Operation(summary = "AI 채팅", description = "루티에게 메시지를 보내 상황 기반 재설계/수정 제안을 받습니다.")
    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(@Valid @RequestBody AiChatRequest req) {
        String sessionId = (req.sessionId() != null && !req.sessionId().isBlank())
                ? req.sessionId()
                : UUID.randomUUID().toString();
        return ResponseEntity.ok(aiChatService.chat(req, sessionId));
    }

    @Operation(summary = "그룹 모임 최적화", description = "프론트가 계산한 멤버 이동 정보를 기반으로 모임 전략을 제안합니다.")
    @PostMapping({"/group/optimize", "/group/routes"})
    public ResponseEntity<GroupRouteResponse> groupRoutes(@Valid @RequestBody GroupRouteRequest req) {
        String sessionId = (req.sessionId() != null && !req.sessionId().isBlank())
                ? req.sessionId()
                : UUID.randomUUID().toString();
        return ResponseEntity.ok(groupRouteService.calculate(req, sessionId));
    }

    @Operation(summary = "세션 정보 조회")
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<SessionInfoResponse> getSession(@PathVariable String sessionId) {
        return ResponseEntity.ok(sessionService.info(sessionId));
    }

    @Operation(summary = "세션 초기화")
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> deleteSession(@PathVariable String sessionId) {
        boolean cleared = sessionService.clear(sessionId);
        return ResponseEntity.ok(Map.of("session_id", sessionId, "cleared", cleared));
    }
}
