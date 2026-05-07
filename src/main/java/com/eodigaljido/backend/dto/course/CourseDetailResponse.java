package com.eodigaljido.backend.dto.course;

import com.eodigaljido.backend.domain.route.Route;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "코스 상세 응답 (경유지 + 리뷰 포함)")
public record CourseDetailResponse(
        @Schema(description = "코스 UUID (식별자)", example = "550e8400-e29b-41d4-a716-446655440000")
        String id,

        @Schema(description = "코스 이름", example = "서울 고궁 투어")
        String title,

        @Schema(description = "코스 설명")
        String meta,

        @Schema(description = "카테고리 (활동 유형)", example = "관광")
        String category,

        @Schema(description = "지역", example = "서울")
        String region,

        @Schema(description = "썸네일 이미지 URL")
        String thumbnail,

        @Schema(description = "조회수", example = "128")
        int views,

        @Schema(description = "전체 소요시간(분)", example = "120")
        Integer overallDurationMinutes,

        @Schema(description = "출발지 이름 (첫 번째 경유지)")
        String departure,

        @Schema(description = "도착지 이름 (마지막 경유지)")
        String arrival,

        @Schema(description = "평점")
        BigDecimal rating,

        @Schema(description = "리뷰 수")
        int reviewCount,

        @ArraySchema(schema = @Schema(implementation = CourseStepResponse.class, description = "경유지(스텝) 목록"))
        List<CourseStepResponse> routeSteps,

        @ArraySchema(schema = @Schema(implementation = ReviewResponse.class, description = "리뷰 목록"))
        List<ReviewResponse> reviews,

        @Schema(description = "작성자 UUID")
        String authorUuid,

        @Schema(description = "작성자 아이디")
        String authorUserId
) {
    public static CourseDetailResponse of(Route route,
                                          List<CourseStepResponse> steps,
                                          List<ReviewResponse> reviews) {
        String departure = steps.isEmpty() ? null : steps.get(0).name();
        String arrival = steps.size() < 2 ? departure : steps.get(steps.size() - 1).name();
        return new CourseDetailResponse(
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
                reviews,
                route.getUser().getUuid(),
                route.getUser().getUserId()
        );
    }
}
