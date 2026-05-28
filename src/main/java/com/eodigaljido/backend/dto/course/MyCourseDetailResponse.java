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

        @Schema(description = "대표 이미지 URL", example = "https://cdn.example.com/courses/550e8400/cover.jpg")
        String thumbnail,

        @Schema(description = "공동 편집(공유) 여부", example = "false")
        boolean collaborative,

        @Schema(description = "연결된 루트 채팅방 UUID (그룹 내 루트인 경우에만 존재, 그 외 null)", example = "null")
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
        List<String> tags,

        @Schema(description = "직전에 복사한 공개 코스 UUID. 직접 제작한 코스면 null")
        String forkSourceCourseId,

        @Schema(description = "포크 체인의 최초 원본 공개 코스 UUID. 직접 제작한 코스면 null")
        String rootForkSourceCourseId,

        @Schema(description = "최초 제작자 UUID")
        String originalAuthorUuid,

        @Schema(description = "최초 제작자 아이디")
        String originalAuthorUserId,

        @Schema(description = "마지막 수정/재공유자 UUID. 직접 제작 코스거나 원작자와 동일하면 null일 수 있음")
        String modifierUuid,

        @Schema(description = "마지막 수정/재공유자 아이디. 직접 제작 코스거나 원작자와 동일하면 null일 수 있음")
        String modifierUserId,

        @Schema(description = "원작자 프로필 공개 여부. 현재 별도 비공개 설정이 없으면 true")
        boolean originalAuthorProfilePublic
) {}
