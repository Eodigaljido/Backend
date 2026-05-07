package com.eodigaljido.backend.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "리뷰 작성 요청")
public record WriteReviewRequest(
        @Schema(description = "작성자 이름 (비로그인 시 사용)", example = "홍길동")
        String userName,

        @NotNull
        @Min(1) @Max(5)
        @Schema(description = "평점 (1~5)", example = "4", requiredMode = Schema.RequiredMode.REQUIRED)
        Integer rating,

        @NotBlank
        @Schema(description = "리뷰 내용", example = "경치가 정말 아름다웠어요!", requiredMode = Schema.RequiredMode.REQUIRED)
        String text
) {}
