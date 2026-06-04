package com.eodigaljido.backend.dto.course;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "내 루트 상세 응답")
public record MyCourseDetailResponse(
        @Schema(description = "루트 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        String uuid,

        @Schema(description = "루트 이름", example = "서울 고궁 투어")
        String title,

        @Schema(description = "대표 이미지 URL")
        String thumbnail,

        @Schema(description = "공동 편집 여부", example = "false")
        boolean collaborative,

        @Schema(description = "공동 편집 충돌 방지 버전", example = "12")
        long version,

        @Schema(description = "마지막 수정 시각")
        LocalDateTime updatedAt,

        @Schema(description = "연결된 루트 채팅방 UUID")
        String chatRoomUuid,

        @Schema(description = "경유지 목록")
        List<StopResponse> stops,

        @Schema(description = "이동 구간 목록")
        List<LegResponse> legs,

        @ArraySchema(arraySchema = @Schema(description = "태그 목록"))
        List<String> tags,

        @Schema(description = "직전 복사 원본 공개 코스 UUID")
        String forkSourceCourseId,

        @Schema(description = "최초 원본 공개 코스 UUID")
        String rootForkSourceCourseId,

        @Schema(description = "최초 작성자 UUID")
        String originalAuthorUuid,

        @Schema(description = "최초 작성자 로그인 ID")
        String originalAuthorUserId,

        @Schema(description = "마지막 수정/공개자 UUID")
        String modifierUuid,

        @Schema(description = "마지막 수정/공개자 로그인 ID")
        String modifierUserId,

        @Schema(description = "원작성자 프로필 공개 여부")
        boolean originalAuthorProfilePublic
) {}
