package com.eodigaljido.backend.dto.route;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "루트 공유 활성화 응답")
public record ShareRouteResponse(
        @Schema(description = "공유 토큰 — GET /routes/shared/{shareToken} 경로에 사용", example = "a3f1c2d4-b5e6-7890-abcd-ef1234567890")
        String shareToken
) {
    public static ShareRouteResponse of(String shareToken) {
        return new ShareRouteResponse(shareToken);
    }
}
