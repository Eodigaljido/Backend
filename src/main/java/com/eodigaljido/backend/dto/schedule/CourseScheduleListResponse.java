package com.eodigaljido.backend.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "코스 약속 목록 응답")
public record CourseScheduleListResponse(
        @Schema(description = "코스 약속 목록")
        List<CourseScheduleResponse> items
) {}
