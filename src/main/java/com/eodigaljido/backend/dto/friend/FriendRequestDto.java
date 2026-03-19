package com.eodigaljido.backend.dto.friend;

import jakarta.validation.constraints.NotBlank;

public record FriendRequestDto(
        @NotBlank(message = "상대방 UUID는 필수입니다.")
        String targetUuid
) {}
