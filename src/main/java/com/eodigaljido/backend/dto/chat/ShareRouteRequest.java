package com.eodigaljido.backend.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "루트 공유 요청")
public record ShareRouteRequest(
        @Schema(description = "공유할 루트 UUID", example = "550e8400-e29b-41d4-a716-446655440000",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank String routeUuid
) {}
