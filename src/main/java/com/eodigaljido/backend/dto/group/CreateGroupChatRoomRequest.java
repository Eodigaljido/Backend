package com.eodigaljido.backend.dto.group;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "모임 채팅방 생성 요청")
public record CreateGroupChatRoomRequest(
        @Schema(description = "채팅방 이름", example = "공지사항")
        @NotBlank @Size(max = 100)
        String name
) {}
