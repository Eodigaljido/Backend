package com.eodigaljido.backend.dto.course;

import com.eodigaljido.backend.domain.route.Route;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "공동 루트 링크 진입 응답")
public record CollaborativeCourseResponse(
        @Schema(description = "루트 UUID")
        String courseUuid,

        @Schema(description = "루트 이름")
        String title,

        @Schema(description = "공동 편집 여부")
        boolean collaborative,

        @Schema(description = "공동 편집 충돌 방지 버전")
        long version,

        @Schema(description = "마지막 수정 시각")
        LocalDateTime updatedAt,

        @Schema(description = "요청자의 역할")
        String myRole,

        @Schema(description = "요청자의 편집 가능 여부")
        boolean canEdit,

        @Schema(description = "소유자 UUID")
        String ownerUuid,

        @Schema(description = "소유자 닉네임")
        String ownerNickname,

        @Schema(description = "현재 멤버 수")
        int memberCount,

        @Schema(description = "연결된 채팅방 UUID")
        String chatRoomUuid,

        @Schema(description = "경유지 목록")
        List<StopResponse> stops,

        @Schema(description = "이동 구간 목록")
        List<LegResponse> legs,

        @ArraySchema(arraySchema = @Schema(description = "태그 목록"))
        List<String> tags
) {
    public static CollaborativeCourseResponse of(Route route,
                                                 String myRole,
                                                 boolean canEdit,
                                                 String ownerNickname,
                                                 int memberCount,
                                                 List<StopResponse> stops,
                                                 List<LegResponse> legs,
                                                 List<String> tags) {
        String chatRoomUuid = route.getChatRoom() != null ? route.getChatRoom().getUuid() : null;
        return new CollaborativeCourseResponse(
                route.getUuid(),
                route.getTitle(),
                route.isCollaborative(),
                route.getVersion(),
                route.getUpdatedAt(),
                myRole,
                canEdit,
                route.getUser().getUuid(),
                ownerNickname,
                memberCount,
                chatRoomUuid,
                stops,
                legs,
                tags
        );
    }
}
