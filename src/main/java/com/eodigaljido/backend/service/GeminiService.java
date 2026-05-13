package com.eodigaljido.backend.service;

import com.eodigaljido.backend.config.AiChatProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.regex.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    private static final String SYSTEM_PROMPT = """
당신은 루티(Ruti)입니다. 사용자의 이동 경로를 상황에 맞게 재설계하고 수정 결정을 돕는 AI 보조자입니다.

[필수 언어 규칙 — 가장 중요]
- 반드시 한국어로만 답변하십시오.
- 중국어, 영어, 일본어는 절대 사용하지 마십시오.
- message와 buttons는 오직 한국어 문자만 포함해야 합니다.

[역할]
- [상황 신호]와 [현재 경로 스냅샷]을 기반으로 재탐색 필요 여부를 설명합니다.
- 비/야간/혼잡 상황에서는 안전성과 편의성을 우선하는 수정안을 제안합니다.
- 출발 전(pre_trip)과 주행 중(in_trip)에 맞는 다른 가이드를 제공합니다.
- 채팅(chat)과 음성(voice) 채널 모두에서 바로 실행 가능한 짧은 지시를 제시합니다.
- 이전 대화 맥락을 반드시 유지하며 답변하세요.

[말투 규칙]
- 사용자를 항상 "사용자님"으로 부르세요.
- 문장 끝은 "~요", "~네요", "~어요", "~해요" 등 따뜻한 경어체로 마무리하세요.

[날씨 규칙]
- 날씨 정보는 [현재 날씨] 섹션에 이미 제공됩니다.
- "날씨를 확인해볼게요" 같은 말을 절대 하지 마세요.

[응답 길이 규칙]
- message는 최대 5줄, 500자 이내로 작성하세요.
- 같은 문장을 반복하지 마세요.

[루트 저장/추가 규칙]
- context에 [루트 DB 저장 완료] 표시가 있으면 "루트를 저장했어요!" 라고 안내하세요.
- context에 [경유지 DB 추가 완료] 표시가 있으면 "경유지를 추가했어요!" 라고 안내하세요.
- 위 표시가 없으면 저장/추가가 완료됐다고 말하지 마세요.

[응답 형식]
반드시 아래 JSON 형식으로만 응답하세요. 다른 텍스트는 절대 포함하지 마세요.

{"message": "한국어 메시지", "buttons": ["버튼1", "버튼2"], "action": "NONE", "payload": {}}

[action 값]
NONE=일반대화 / SHOW_ROUTE=프론트의 지도 갱신 필요 / REQUEST_INFO=정보요청 / RECOMMEND=재탐색/수정 제안

[버튼]
- 한국어로 최대 4개. 없으면 [].
""";

    private final AiChatProperties props;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.create();

    @SuppressWarnings("unchecked")
    public Map<String, Object> chat(List<Map<String, String>> history, String userMessage, String contextText) {
        AiChatProperties.Gemini gemini = props.getGemini();
        if (gemini.getApiKey() == null || gemini.getApiKey().isBlank()) {
            return fallbackResponse("Gemini API 키가 설정되지 않았어요.");
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        for (Map<String, String> msg : history) {
            messages.add(Map.of("role", msg.get("role"), "content", msg.get("content")));
        }

        String fullUserMsg = contextText != null && !contextText.isBlank()
                ? contextText + "\n\n사용자 메시지: " + userMessage
                : userMessage;
        messages.add(Map.of("role", "user", "content", fullUserMsg));

        Map<String, Object> requestBody = Map.of(
                "model",      gemini.getModel(),
                "messages",   messages,
                "max_tokens", gemini.getMaxTokens()
        );

        try {
            Map<String, Object> response = restClient.post()
                    .uri(gemini.getBaseUrl() + "chat/completions")
                    .header("Authorization", "Bearer " + gemini.getApiKey())
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response == null) return fallbackResponse("Gemini 응답이 없어요.");

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) return fallbackResponse("Gemini 응답이 비어있어요.");

            Map<String, Object> messageObj = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) messageObj.get("content");
            return parseJsonResponse(content);

        } catch (Exception e) {
            log.error("[Gemini] 호출 실패: {}", e.getMessage());
            return fallbackResponse("일시적으로 오류가 발생했어요. 다시 시도해 주세요.");
        }
    }

    private Map<String, Object> parseJsonResponse(String text) {
        if (text == null || text.isBlank()) return fallbackResponse("응답이 비어있어요.");
        try {
            // ```json ... ``` 블록 추출 시도
            Matcher m = Pattern.compile("```(?:json)?\\s*(\\{.*?\\})\\s*```", Pattern.DOTALL).matcher(text);
            if (m.find()) {
                return readMap(m.group(1));
            }
            // 중괄호 직접 추출
            m = Pattern.compile("\\{.*\\}", Pattern.DOTALL).matcher(text);
            if (m.find()) {
                return readMap(m.group(0));
            }
        } catch (Exception e) {
            log.warn("[Gemini] JSON 파싱 실패: {}", e.getMessage());
        }
        return Map.of(
                "message", sanitize(text),
                "buttons", List.of(),
                "action",  "NONE",
                "payload", Map.of()
        );
    }

    private Map<String, Object> fallbackResponse(String msg) {
        return Map.of("message", msg, "buttons", List.of(), "action", "NONE", "payload", Map.of());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMap(String json) throws Exception {
        return objectMapper.readValue(json, Map.class);
    }

    private String sanitize(String text) {
        String t = text.strip();
        if (t.startsWith("{")) {
            try {
                Map<?, ?> inner = objectMapper.readValue(t, Map.class);
                if (inner.containsKey("message")) return inner.get("message").toString();
            } catch (JsonProcessingException ignored) {}
        }
        return t;
    }
}
