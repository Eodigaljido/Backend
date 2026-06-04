package com.eodigaljido.backend.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "공동 루트 채팅방 연결 요청")
public record LinkCourseChatRoomRequest(
        @NotBlank
        @Schema(description = "연결할 채팅방 UUID", requiredMode = Schema.RequiredMode.REQUIRED)
        String chatRoomUuid
) {}
