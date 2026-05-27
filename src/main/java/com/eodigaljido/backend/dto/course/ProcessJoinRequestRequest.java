package com.eodigaljido.backend.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "공동 루트 입장 요청 처리")
public record ProcessJoinRequestRequest(
        @NotNull
        @Schema(
                description = "처리 action. `APPROVE`(승인) 또는 `REJECT`(거절)",
                example = "APPROVE",
                allowableValues = {"APPROVE", "REJECT"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Action action
) {
    public enum Action { APPROVE, REJECT }
}
