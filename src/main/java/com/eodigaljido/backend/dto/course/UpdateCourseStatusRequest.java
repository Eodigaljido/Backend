package com.eodigaljido.backend.dto.course;

import com.eodigaljido.backend.domain.route.Route.RouteStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "내 루트 상태 변경 요청")
public record UpdateCourseStatusRequest(
        @Schema(description = "변경할 상태 (DRAFT | PUBLISHED)", example = "PUBLISHED", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        RouteStatus status
) {}
