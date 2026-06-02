package com.eodigaljido.backend.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "가장 가까운 코스 약속 응답")
public record CourseScheduleNearestResponse(
        @Schema(description = "가장 가까운 미래 약속. 없으면 null입니다.", nullable = true)
        CourseScheduleResponse item
) {}
