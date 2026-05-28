package com.eodigaljido.backend.dto.group;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "모임 가입 응답")
public record GroupJoinResponse(
        @Schema(description = "가입 상태: JOINED | PENDING", example = "JOINED")
        String status
) {}
