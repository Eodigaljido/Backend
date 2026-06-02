package com.eodigaljido.backend.dto.schedule;

import java.util.List;

public record CourseScheduleListResponse(
        List<CourseScheduleResponse> items
) {}
