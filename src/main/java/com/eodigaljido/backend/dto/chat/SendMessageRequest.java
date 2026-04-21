package com.eodigaljido.backend.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "메시지 전송 요청")
public record SendMessageRequest(
        @Schema(description = "메시지 내용 (최대 2000자)", example = "안녕하세요!", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 2000) String content,

        @Schema(description = "메시지 타입 (TEXT, ROUTE). 생략 시 TEXT", example = "TEXT")
        String messageType,

        @Schema(description = "공유할 코스 UUID (messageType=ROUTE 시 사용)", example = "550e8400-e29b-41d4-a716-446655440000")
        String routeUuid,

        @Schema(description = "@멘션한 사용자 UUID 목록", example = "[\"uuid1\", \"uuid2\"]")
        List<String> mentionedUserUuids
) {}
