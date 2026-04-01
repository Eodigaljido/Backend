package com.eodigaljido.backend.dto.friend;

import jakarta.validation.constraints.NotNull;

public record FriendRespondDto(
        @NotNull(message = "수락 여부는 필수입니다.")
        Boolean accept
) {}
