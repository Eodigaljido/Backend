package com.eodigaljido.backend.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공동 루트 초대 요청")
public record CollaborativeInviteRequest(
        @Schema(
                description = "초대할 유저 아이디. 생략하면 초대 링크만 활성화합니다.",
                example = "jane456"
        )
        String userId,

        @Schema(
                description = """
                        초대 방식.
                        - `false` (기본값): 초대 즉시 멤버로 추가됩니다 (직접 입장).
                        - `true`: 입장 요청 상태로 생성되며, 루트 소유자가 승인해야 입장됩니다 (승인 필요).
                        """,
                example = "false",
                defaultValue = "false"
        )
        Boolean requiresApproval
) {}
