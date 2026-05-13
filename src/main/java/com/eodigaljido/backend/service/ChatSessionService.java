package com.eodigaljido.backend.service;

import com.eodigaljido.backend.config.AiChatProperties;
import com.eodigaljido.backend.dto.ai.SessionInfoResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ChatSessionService {

    private final AiChatProperties props;

    // session_id → list of {role, content} maps
    private final ConcurrentHashMap<String, List<Map<String, String>>> sessions    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long>                       timestamps  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, double[]>                   locations   = new ConcurrentHashMap<>();

    public List<Map<String, String>> getHistory(String sessionId) {
        evictExpired();
        return sessions.getOrDefault(sessionId, List.of());
    }

    public void addTurn(String sessionId, String userMsg, String assistantMsg) {
        sessions.computeIfAbsent(sessionId, k -> new ArrayList<>());
        List<Map<String, String>> history = sessions.get(sessionId);
        history.add(Map.of("role", "user", "content", userMsg));
        history.add(Map.of("role", "assistant", "content", assistantMsg));

        int maxMessages = props.getSession().getMaxMessages();
        while (history.size() > maxMessages) history.remove(0);

        timestamps.put(sessionId, System.currentTimeMillis());
    }

    public void setLocation(String sessionId, double lat, double lng) {
        locations.put(sessionId, new double[]{lat, lng});
        timestamps.put(sessionId, System.currentTimeMillis());
    }

    public double[] getLocation(String sessionId) {
        return locations.get(sessionId);
    }

    public SessionInfoResponse info(String sessionId) {
        List<Map<String, String>> history = sessions.getOrDefault(sessionId, List.of());
        Long ts = timestamps.get(sessionId);
        return new SessionInfoResponse(
                sessionId,
                history.size() / 2,
                history.size(),
                ts != null ? ts / 1000L : null
        );
    }

    public boolean clear(String sessionId) {
        boolean existed = sessions.containsKey(sessionId);
        sessions.remove(sessionId);
        timestamps.remove(sessionId);
        locations.remove(sessionId);
        return existed;
    }

    @Scheduled(fixedDelay = 300_000) // 5분마다 정리
    public void evictExpired() {
        long ttlMs = props.getSession().getTtlSeconds() * 1000L;
        long now   = System.currentTimeMillis();
        timestamps.forEach((id, ts) -> {
            if (now - ts > ttlMs) clear(id);
        });
    }
}
