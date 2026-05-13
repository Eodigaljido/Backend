package com.eodigaljido.backend.service;

import com.eodigaljido.backend.core.IntentParser;
import com.eodigaljido.backend.dto.ai.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatService {

    private final IntentParser intentParser;
    private final GeminiService geminiService;
    private final ChatSessionService sessionService;

    public AiChatResponse chat(AiChatRequest req, String resolvedSessionId) {
        String msg = req.message();

        // 세션 위치 갱신
        Double requestLat = req.lat();
        Double requestLng = req.lng();
        if (requestLat != null && requestLng != null) {
            sessionService.setLocation(resolvedSessionId, requestLat.doubleValue(), requestLng.doubleValue());
        }
        double[] loc = sessionService.getLocation(resolvedSessionId);
        Double lat = (loc != null) ? Double.valueOf(loc[0]) : requestLat;
        Double lng = (loc != null) ? Double.valueOf(loc[1]) : requestLng;

        // 의도 추출
        IntentResult intent = intentParser.extract(msg);
        log.info("[AI Chat] session={} intent destinations={} freeKeywords={}",
                resolvedSessionId, intent.destinations(), intent.freeKeywords());

        // 컨텍스트 구성
        String contextText = buildContext(req, intent, lat, lng);

        // Gemini 호출
        List<Map<String, String>> history = sessionService.getHistory(resolvedSessionId);
        Map<String, Object> geminiResult = geminiService.chat(history, msg, contextText);

        // 백엔드 정책 힌트를 payload에 주입 (프론트는 이 정보로 실제 경로 수정/UI 반영)
        Map<String, Object> policy = buildRoutePolicy(req, intent);
        Map<String, Object> payload = mergePayload(geminiResult.get("payload"), policy);

        // 세션 저장
        String assistantMsg = (String) geminiResult.getOrDefault("message", "");
        sessionService.addTurn(resolvedSessionId, msg, assistantMsg);

        @SuppressWarnings("unchecked")
        List<String> buttons = (List<String>) geminiResult.getOrDefault("buttons", List.of());
        String action = (String) geminiResult.getOrDefault("action", "NONE");
        if (Boolean.TRUE.equals(policy.get("rerouteRecommended")) && "NONE".equals(action)) action = "RECOMMEND";

        RutiResponse response = RutiResponse.builder()
                .message(assistantMsg)
                .buttons(buttons != null ? buttons : List.of())
                .action(action)
                .payload(payload)
                .build();

        return new AiChatResponse(response, resolvedSessionId);
    }

    private String buildContext(AiChatRequest req, IntentResult intent, Double lat, Double lng) {
        StringBuilder sb = new StringBuilder();
        sb.append("[채널]\n").append(req.channel()).append("\n\n");
        sb.append("[단계]\n").append(req.phase()).append("\n\n");

        if (req.contextSignals() != null) {
            ContextSignalInput s = req.contextSignals();
            sb.append("[상황 신호]\n");
            sb.append("- 비: ").append(s.raining()).append("\n");
            sb.append("- 야간: ").append(s.nighttime()).append("\n");
            sb.append("- 혼잡: ").append(s.crowded()).append("\n");
            sb.append("- 재탐색 요청: ").append(s.rerouteRequested()).append("\n\n");
        }

        if (req.routeSnapshot() != null) {
            RouteSnapshotInput snapshot = req.routeSnapshot();
            sb.append("[현재 경로 스냅샷]\n");
            sb.append("- 목적지: ").append(defaultText(snapshot.destination())).append("\n");
            sb.append("- 이동수단: ").append(defaultText(snapshot.transportMode())).append("\n");
            Integer remainingMinutes = snapshot.remainingMinutes();
            sb.append("- 남은 시간(분): ")
                    .append(remainingMinutes != null ? remainingMinutes.toString() : "-1")
                    .append("\n");
            if (!snapshot.waypoints().isEmpty()) {
                sb.append("- 경유지 목록:\n");
                for (int i = 0; i < snapshot.waypoints().size(); i++) {
                    RouteWaypointInput wp = snapshot.waypoints().get(i);
                    sb.append("  ").append(i + 1).append(") ")
                            .append(defaultText(wp.name()))
                            .append(" | ").append(defaultText(wp.address()))
                            .append("\n");
                }
            }
            sb.append("\n");
        }

        if (lat != null && lng != null) {
            sb.append("[사용자 위치]\n");
            sb.append("- lat=").append(lat).append(", lng=").append(lng).append("\n\n");
        }

        sb.append("[의도 분석]\n");
        sb.append("- 목적지 후보: ").append(intent.destinations()).append("\n");
        sb.append("- 테마: ").append(defaultText(intent.theme())).append("\n");
        sb.append("- 자유 키워드: ").append(intent.freeKeywords()).append("\n");
        return sb.toString();
    }

    private Map<String, Object> buildRoutePolicy(AiChatRequest req, IntentResult intent) {
        ContextSignalInput signals = req.contextSignals();
        boolean raining = signals != null && signals.raining();
        boolean nighttime = signals != null && signals.nighttime();
        boolean crowded = signals != null && signals.crowded();
        boolean rerouteRequested = signals != null && signals.rerouteRequested();

        List<String> priorities = new ArrayList<>();
        if (raining) priorities.add("INDOOR_PRIORITY");
        if (nighttime) priorities.add("SAFE_AND_BRIGHT_ROAD");
        if (crowded) priorities.add("AVOID_CROWD");

        boolean reroute = rerouteRequested || !priorities.isEmpty();

        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("rerouteRecommended", reroute);
        policy.put("priorities", priorities);
        policy.put("intentKeywords", intent.freeKeywords());
        policy.put("phase", req.phase());
        policy.put("channel", req.channel());
        return policy;
    }

    private Map<String, Object> mergePayload(Object geminiPayloadObj, Map<String, Object> policy) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (geminiPayloadObj instanceof Map<?, ?> map) {
            map.forEach((k, v) -> merged.put(String.valueOf(k), v));
        }
        merged.put("routePolicy", policy);
        return merged;
    }

    private String defaultText(String text) {
        return (text == null || text.isBlank()) ? "-" : text;
    }
}
