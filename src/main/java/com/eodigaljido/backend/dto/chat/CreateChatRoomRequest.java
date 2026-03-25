package com.eodigaljido.backend.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateChatRoomRequest(
        @NotEmpty List<@NotBlank String> memberUuids,
        @Size(max = 100, message = "채팅방 이름은 100자를 초과할 수 없습니다.") String name
) {}
