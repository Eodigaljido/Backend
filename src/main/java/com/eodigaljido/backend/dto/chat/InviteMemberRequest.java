package com.eodigaljido.backend.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "채팅방 멤버 초대 요청")
public record InviteMemberRequest(
        @Schema(description = "초대할 유저 아이디", example = "jane456", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String userId
) {}
