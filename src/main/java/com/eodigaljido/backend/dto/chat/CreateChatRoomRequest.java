package com.eodigaljido.backend.dto.chat;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateChatRoomRequest(
        @NotEmpty(message = "초대할 멤버를 1명 이상 입력해야 합니다.")
        List<String> memberUuids,
        String name
) {}
