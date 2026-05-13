package com.eodigaljido.backend.service;

import com.eodigaljido.backend.dto.ai.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupRouteService {

    private final GeminiService geminiService;
    private final ChatSessionService sessionService;

    public GroupRouteResponse calculate(GroupRouteRequest req, String resolvedSessionId) {
        List<MemberRoute> memberRoutes = computeRoutes(req);
        int maxDuration = memberRoutes.stream()
                .mapToInt(MemberRoute::totalDurationMinutes)
                .max().orElse(0);
        int minDuration = memberRoutes.stream()
                .mapToInt(MemberRoute::totalDurationMinutes)
                .filter(v -> v > 0)
                .min().orElse(0);

        String context = buildGroupContext(memberRoutes, req.destination(), maxDuration, minDuration);
        List<Map<String, String>> history = sessionService.getHistory(resolvedSessionId);
        Map<String, Object> geminiResult = geminiService.chat(history, "그룹 모임 경로를 안내해줘", context);

        String message = (String) geminiResult.getOrDefault("message", "");
        if (!message.isBlank()) {
            sessionService.addTurn(resolvedSessionId, "그룹 모임 경로", message);
        }

        @SuppressWarnings("unchecked")
        List<String> buttons = (List<String>) geminiResult.getOrDefault("buttons", List.of());

        return GroupRouteResponse.builder()
                .message(message)
                .buttons(buttons != null ? buttons : List.of())
                .memberRoutes(memberRoutes)
                .maxDurationMinutes(maxDuration)
                .sessionId(resolvedSessionId)
                .build();
    }

    private List<MemberRoute> computeRoutes(GroupRouteRequest req) {
        return req.members().stream().map(member ->
                MemberRoute.builder()
                        .memberName(member.name())
                        .origin(member.origin())
                        .transportMode(member.transportMode())
                        .totalDurationMinutes(Math.max(member.estimatedDurationMinutes(), 0))
                        .totalDistanceKm(Math.max(member.estimatedDistanceKm(), 0.0))
                        .build()
        ).collect(Collectors.toList());
    }

    private String buildGroupContext(List<MemberRoute> routes, String destination, int maxDuration, int minDuration) {
        StringBuilder sb = new StringBuilder("[그룹 모임 경로 — 목적지: ").append(destination).append("]\n");
        for (MemberRoute mr : routes) {
            if (mr.totalDurationMinutes() > 0) {
                sb.append("  ").append(mr.memberName()).append(" (").append(mr.origin()).append(", ")
                  .append(mr.transportMode()).append("): ")
                  .append(mr.totalDurationMinutes()).append("분, ").append(mr.totalDistanceKm()).append("km\n");
            } else {
                sb.append("  ").append(mr.memberName()).append(" (").append(mr.origin()).append(", ")
                  .append(mr.transportMode()).append("): 경로 미조회\n");
            }
        }
        List<Integer> durations = routes.stream()
                .map(MemberRoute::totalDurationMinutes).filter(d -> d > 0).collect(Collectors.toList());
        if (!durations.isEmpty()) {
            int max = durations.stream().mapToInt(i -> i).max().orElse(0);
            int avg = (int) durations.stream().mapToInt(i -> i).average().orElse(0);
            sb.append("\n최장 이동 시간: ").append(max).append("분 / 평균: ").append(avg).append("분\n");
        }
        int gap = (minDuration == 0) ? 0 : maxDuration - minDuration;
        sb.append("시간 편차: ").append(gap).append("분\n");
        sb.append("\n각 멤버의 이동 시간 편차를 줄이는 모임 시각 조정안과 합리적인 대안 목적지 기준을 한국어로 안내하세요.");
        return sb.toString();
    }
}
