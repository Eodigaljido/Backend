package com.eodigaljido.backend.dto.routehistory;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "루트 기록 목록 항목")
public record RouteHistoryItemResponse(

        @Schema(description = "루트 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        String courseUuid,

        @Schema(description = "루트 전용 채팅방 UUID", example = "660e8400-e29b-41d4-a716-446655440001")
        String routeChatRoomUuid,

        @Schema(description = "루트 기록방 이름 (루트 제목)", example = "서울 송파구")
        String name,

        @Schema(description = "참가 인원 수 (편집에 참여한 전체 인원)", example = "3")
        long participantCount
) {}
