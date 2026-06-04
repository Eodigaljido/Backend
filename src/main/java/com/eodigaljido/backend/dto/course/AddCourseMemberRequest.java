package com.eodigaljido.backend.dto.course;

import com.eodigaljido.backend.domain.route.CourseMember;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공동 루트 멤버 추가 요청")
public record AddCourseMemberRequest(
        @Schema(description = "추가할 사용자 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        String userUuid,

        @Schema(description = "추가할 사용자 로그인 ID", example = "jimin")
        String userId,

        @Schema(description = "부여할 역할. 생략하면 EDITOR", example = "EDITOR")
        CourseMember.Role role
) {}
