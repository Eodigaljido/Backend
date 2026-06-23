package com.eodigaljido.backend.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공동편집 화면을 현재 보고 있는 멤버")
public record PresenceMemberResponse(
        @Schema(description = "사용자 UUID")
        String userUuid,

        @Schema(description = "닉네임")
        String nickname,

        @Schema(description = "프로필 이미지 URL (없으면 null)")
        String profileImageUrl
) {}
