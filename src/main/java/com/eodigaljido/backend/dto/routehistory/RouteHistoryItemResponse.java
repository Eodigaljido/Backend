package com.eodigaljido.backend.dto.routehistory;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "루트 기록 목록 항목. 피드 조회 시 courseUuid를 /api/route-history/{courseId}/feed 에 사용하세요.")
public record RouteHistoryItemResponse(

        @Schema(description = "루트 UUID. 피드 조회 시 {courseId} 로 사용", example = "550e8400-e29b-41d4-a716-446655440000")
        String courseUuid,

        @Schema(description = "루트 전용 채팅방 UUID (ROUTE 타입)", example = "660e8400-e29b-41d4-a716-446655440001")
        String routeChatRoomUuid,

        @Schema(description = "루트 이름", example = "서울 송파구")
        String name,

        @Schema(description = "공동 편집에 참여한 전체 인원 수 (탈퇴한 멤버 포함)", example = "3")
        long participantCount
) {}
