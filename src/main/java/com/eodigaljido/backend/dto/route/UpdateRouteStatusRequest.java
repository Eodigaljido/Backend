package com.eodigaljido.backend.dto.route;

import com.eodigaljido.backend.domain.route.Route.RouteStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "루트 상태 변경 요청")
public record UpdateRouteStatusRequest(
        @NotNull
        @Schema(description = "변경할 상태 (DRAFT | PUBLISHED). DELETED로는 변경 불가 — 삭제는 DELETE /routes/{uuid} 사용",
                example = "PUBLISHED", requiredMode = Schema.RequiredMode.REQUIRED)
        RouteStatus status
) {}
