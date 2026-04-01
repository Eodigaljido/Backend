package com.eodigaljido.backend.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "채팅방 이름 변경 요청")
public record UpdateChatRoomNameRequest(
        @Schema(description = "새 채팅방 이름 (최대 100자, null 또는 빈 문자열 입력 시 멤버 닉네임 조합으로 자동 초기화)",
                example = "새 채팅방 이름", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 100, message = "채팅방 이름은 100자를 초과할 수 없습니다.") String name
) {}
