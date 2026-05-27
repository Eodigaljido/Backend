package com.eodigaljido.backend.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공동 루트 참여 요청 결과")
public record JoinRouteResponse(
        @Schema(
                description = """
                        처리 결과.
                        - `JOINED`: 즉시 멤버로 추가됨.
                        - `PENDING`: 소유자 승인 대기 중.
                        """,
                example = "JOINED",
                allowableValues = {"JOINED", "PENDING"}
        )
        String status,

        @Schema(description = "입장한 채팅방 UUID. `PENDING`인 경우 null.", example = "550e8400-e29b-41d4-a716-446655440000")
        String chatRoomUuid
) {}
