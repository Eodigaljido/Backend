package com.eodigaljido.backend.dto.course;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "코스 목록 페이지 응답")
public record CoursePageResponse(
        @ArraySchema(schema = @Schema(implementation = CourseItemResponse.class, description = "코스 목록"))
        List<CourseItemResponse> items,

        @Schema(description = "페이징 정보")
        PageInfo pageInfo
) {}
