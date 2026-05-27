package com.eodigaljido.backend.dto.course;

import com.eodigaljido.backend.domain.route.Route;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "코스 목록 아이템 (상세 제외)")
public record CourseItemResponse(
        @Schema(description = "코스 UUID (식별자)", example = "550e8400-e29b-41d4-a716-446655440000")
        String id,

        @Schema(description = "코스 이름", example = "서울 고궁 투어")
        String title,

        @Schema(description = "코스 설명", example = "경복궁, 창덕궁, 창경궁을 잇는 고궁 탐방 루트")
        String meta,

        @Schema(description = "카테고리 (활동 유형)", example = "관광")
        String category,

        @Schema(description = "지역", example = "서울")
        String region,

        @Schema(description = "썸네일 이미지 URL", example = "https://example.com/thumbnail.jpg")
        String thumbnail,

        @Schema(description = "조회수", example = "128")
        int views,

        @Schema(description = "전체 소요시간(분)", example = "120")
        Integer overallDurationMinutes,

        @Schema(description = "출발지 이름 (첫 번째 경유지)", example = "경복궁")
        String departure,

        @Schema(description = "도착지 이름 (마지막 경유지)", example = "창경궁")
        String arrival,

        @Schema(description = "평점", example = "4.50")
        BigDecimal rating,

        @Schema(description = "리뷰 수", example = "12")
        int reviewCount,

        @ArraySchema(schema = @Schema(implementation = CourseStepResponse.class, description = "경유지(스텝) 목록"))
        List<CourseStepResponse> routeSteps,

        @ArraySchema(
                arraySchema = @Schema(description = "태그 목록 (없으면 빈 배열)", example = "[\"산책\", \"카페\"]"),
                schema = @Schema(description = "태그", example = "산책",
                        allowableValues = {"산책","카페","맛집","데이트","관광","야경","쇼핑","역사","해변","가족","운동","반려동물"})
        )
        List<String> tags,

        @Schema(description = "작성자 UUID")
        String authorUuid,

        @Schema(description = "작성자 아이디")
        String authorUserId,

        @Schema(description = "로그인 사용자가 이 코스를 저장했는지 여부. 비로그인 시 false.", example = "false")
        boolean savedByMe
) {
    public static CourseItemResponse of(Route route, List<CourseStepResponse> steps) {
        return of(route, steps, false);
    }

    public static CourseItemResponse of(Route route, List<CourseStepResponse> steps, boolean savedByMe) {
        String departure = steps.isEmpty() ? null : steps.get(0).name();
        String arrival = steps.size() < 2 ? departure : steps.get(steps.size() - 1).name();
        return new CourseItemResponse(
                route.getUuid(),
                route.getTitle(),
                route.getDescription(),
                route.getActivityType(),
                route.getRegion(),
                route.getThumbnailUrl(),
                route.getViews(),
                route.getEstimatedTime(),
                departure,
                arrival,
                route.getAverageRating(),
                route.getReviewCount(),
                steps,
                route.getTags() != null ? List.copyOf(route.getTags()) : List.of(),
                route.getUser().getUuid(),
                route.getUser().getUserId(),
                savedByMe
        );
    }
}
