package com.eodigaljido.backend.dto.group;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "모임 가입 요청 처리")
public record ProcessGroupJoinRequest(
        @Schema(description = "처리 action: APPROVE | REJECT", example = "APPROVE")
        @NotNull
        Action action
) {
    public enum Action { APPROVE, REJECT }
}
