package com.eodigaljido.backend.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공동 루트 초대 응답")
public record CollaborativeInviteResponse(
        @Schema(description = "루트 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        String courseId,

        @Schema(description = "앱/웹에서 사용할 초대 경로", example = "/routes/collaborative/550e8400-e29b-41d4-a716-446655440000")
        String invitePath,

        @Schema(description = "연결된 채팅방 UUID")
        String chatRoomUuid,

        @Schema(description = "초대된 유저 아이디. 링크만 활성화한 경우 null", example = "jane456")
        String invitedUserId,

        @Schema(
                description = """
                        초대 방식.
                        - `false`: 초대 즉시 멤버로 추가됨 (직접 입장).
                        - `true`: 입장 요청 상태로 생성됨. 소유자 승인 후 입장 가능.
                        """,
                example = "false"
        )
        boolean requiresApproval,

        @Schema(description = "만료 시각. 현재 최소 구현은 만료 없음(null)")
        String expiresAt
) {
    public static CollaborativeInviteResponse of(String courseId, String chatRoomUuid,
                                                  String invitedUserId, boolean requiresApproval) {
        return new CollaborativeInviteResponse(
                courseId,
                "/routes/collaborative/" + courseId,
                chatRoomUuid,
                invitedUserId,
                requiresApproval,
                null
        );
    }
}
