package com.eodigaljido.backend.dto.course;

import com.eodigaljido.backend.domain.route.RouteReview;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "코스 리뷰")
public record ReviewResponse(
        @Schema(description = "리뷰 ID", example = "1")
        Long id,

        @Schema(description = "작성자 이름", example = "홍길동")
        String userName,

        @Schema(description = "평점 (1~5)", example = "4")
        int rating,

        @Schema(description = "리뷰 내용", example = "경치가 정말 아름다웠어요!")
        String text,

        @Schema(description = "작성일시", example = "2026-05-01T12:00:00")
        LocalDateTime date
) {
    public static ReviewResponse from(RouteReview review) {
        String name = review.getUserName() != null
                ? review.getUserName()
                : (review.getUser() != null ? review.getUser().getUserId() : "익명");
        return new ReviewResponse(
                review.getId(),
                name,
                review.getRating(),
                review.getText(),
                review.getCreatedAt()
        );
    }
}
