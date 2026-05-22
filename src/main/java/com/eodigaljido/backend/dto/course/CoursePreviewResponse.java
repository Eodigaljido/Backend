package com.eodigaljido.backend.dto.course;

import com.eodigaljido.backend.domain.route.Route;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "코스 공유 링크 preview 응답 (비로그인 허용)")
public record CoursePreviewResponse(
        @Schema(description = "코스 UUID", example = "7ecc5401-xxxx-xxxx-xxxx-xxxxxxxxxxxx")
        String courseId,

        @Schema(description = "코스 이름", example = "한강 데이트 코스")
        String title,

        @Schema(description = "지역", example = "서울")
        String region,

        @Schema(description = "카테고리(활동 유형)", example = "데이트")
        String category,

        @Schema(description = "소요시간 레이블", example = "약 3시간")
        String durationLabel,

        @Schema(description = "썸네일 이미지 URL (HTTPS)")
        String thumbnailUrl,

        @Schema(description = "출발지 이름")
        String departure,

        @Schema(description = "도착지 이름")
        String arrival,

        @Schema(description = "태그 목록 (최대 4개)")
        List<String> tags,

        @Schema(description = "저장 수")
        long saveCount,

        @Schema(description = "평점")
        BigDecimal rating,

        @Schema(description = "공개 여부")
        boolean isPublic
) {
    public static CoursePreviewResponse of(Route route, List<String> tags,
                                           String departure, String arrival,
                                           long saveCount) {
        return new CoursePreviewResponse(
                route.getUuid(),
                route.getTitle(),
                route.getRegion(),
                route.getActivityType(),
                formatDuration(route.getEstimatedTime()),
                route.getThumbnailUrl(),
                departure,
                arrival,
                tags,
                saveCount,
                route.getAverageRating(),
                route.isShared()
        );
    }

    private static String formatDuration(Integer minutes) {
        if (minutes == null || minutes <= 0) return null;
        if (minutes < 60) return "약 " + minutes + "분";
        int h = minutes / 60;
        int m = minutes % 60;
        return m == 0 ? "약 " + h + "시간" : "약 " + h + "시간 " + m + "분";
    }
}
