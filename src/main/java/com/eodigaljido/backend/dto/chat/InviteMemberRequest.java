package com.eodigaljido.backend.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "채팅방 멤버 초대 요청")
public record InviteMemberRequest(
        @Schema(description = "초대할 유저의 UUID", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String userUuid
) {}
