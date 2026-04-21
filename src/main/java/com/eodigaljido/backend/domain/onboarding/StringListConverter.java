package com.eodigaljido.backend.domain.onboarding;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) return null;
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("List<String> 직렬화 실패", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        // JSON 배열 형식이 아닌 기존 plain string 값은 단일 원소 리스트로 감쌈
        String trimmed = dbData.trim();
        if (!trimmed.startsWith("[")) {
            return List.of(trimmed);
        }
        try {
            return MAPPER.readValue(trimmed, List.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("List<String> 역직렬화 실패", e);
        }
    }
}
