package com.eodigaljido.backend.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "페이징 정보")
public record PageInfo(
        @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
        int page,

        @Schema(description = "페이지 크기", example = "20")
        int size,

        @Schema(description = "전체 항목 수", example = "100")
        long total,

        @Schema(description = "전체 페이지 수", example = "5")
        int totalPages
) {}
