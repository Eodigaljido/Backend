package com.eodigaljido.backend.dto.course;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "공동편집 presence 응답")
public record CoursePresenceResponse(
        @ArraySchema(schema = @Schema(implementation = PresenceMemberResponse.class, description = "현재 편집 화면을 보고 있는 멤버 목록"))
        List<PresenceMemberResponse> members
) {}
