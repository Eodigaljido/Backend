package com.eodigaljido.backend.dto.course;

import com.eodigaljido.backend.domain.route.Route;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "공동 루트 링크 진입 응답")
public record CollaborativeCourseResponse(
        @Schema(description = "루트 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        String uuid,

        @Schema(description = "루트 이름", example = "서울 고궁 투어")
        String title,

        @Schema(description = "공동 편집 링크 활성 여부", example = "true")
        boolean collaborative,

        @Schema(description = "현재 로그인 사용자의 편집 가능 여부", example = "true")
        boolean canEdit,

        @Schema(description = "연결된 채팅방 UUID (없으면 null)")
        String chatRoomUuid,

        @Schema(description = "경유지 목록")
        List<StopResponse> stops,

        @Schema(description = "이동 구간 목록")
        List<LegResponse> legs,

        @ArraySchema(
                arraySchema = @Schema(description = "태그 목록 (없으면 빈 배열)", example = "[\"산책\", \"카페\"]"),
                schema = @Schema(description = "태그", example = "산책",
                        allowableValues = {"산책","카페","맛집","데이트","관광","야경","쇼핑","역사","해변","가족","운동","반려동물"})
        )
        List<String> tags
) {
    public static CollaborativeCourseResponse of(Route route,
                                                 boolean canEdit,
                                                 List<StopResponse> stops,
                                                 List<LegResponse> legs,
                                                 List<String> tags) {
        String chatRoomUuid = route.getChatRoom() != null ? route.getChatRoom().getUuid() : null;
        return new CollaborativeCourseResponse(
                route.getUuid(),
                route.getTitle(),
                route.isCollaborative(),
                canEdit,
                chatRoomUuid,
                stops,
                legs,
                tags
        );
    }
}
