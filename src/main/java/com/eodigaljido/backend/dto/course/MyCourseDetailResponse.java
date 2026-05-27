package com.eodigaljido.backend.dto.course;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "내 루트 상세 응답 (stops + legs 포함)")
public record MyCourseDetailResponse(
        @Schema(description = "루트 UUID (생성 ID)", example = "550e8400-e29b-41d4-a716-446655440000")
        String uuid,

        @Schema(description = "루트 이름", example = "서울 고궁 투어")
        String title,

        @Schema(description = "공동 편집(공유) 여부", example = "false")
        boolean collaborative,

        @Schema(description = "연결된 루트 채팅방 UUID (공동 루트인 경우에만 존재, 그 외 null)", example = "null")
        String chatRoomUuid,

        @Schema(
                description = """
                        공동 루트 입장 승인 방식.
                        - `false`: 초대 즉시 멤버로 추가됩니다.
                        - `true`: 소유자가 승인해야 입장됩니다.
                        공동 루트가 아닌 경우 항상 `false`입니다.
                        """,
                example = "false"
        )
        boolean requiresApproval,

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
) {}
