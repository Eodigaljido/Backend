package com.eodigaljido.backend.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공동 루트 초대 요청")
public record CollaborativeInviteRequest(
        @Schema(
                description = "초대할 유저 아이디. 생략하면 초대 링크만 활성화합니다.",
                example = "jane456"
        )
        String userId
) {}
