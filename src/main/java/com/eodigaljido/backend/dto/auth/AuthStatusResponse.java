package com.eodigaljido.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "현재 요청의 인증 상태 조회 응답")
public record AuthStatusResponse(
        @Schema(description = "유효한 액세스 토큰과 활성 사용자로 인증되었는지 여부", example = "true")
        boolean authenticated
) {
}
