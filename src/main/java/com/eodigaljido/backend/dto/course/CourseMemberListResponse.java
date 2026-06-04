package com.eodigaljido.backend.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "공동 루트 멤버 목록 응답")
public record CourseMemberListResponse(
        @Schema(description = "멤버 목록")
        List<CollaborativeMemberResponse> items
) {}
