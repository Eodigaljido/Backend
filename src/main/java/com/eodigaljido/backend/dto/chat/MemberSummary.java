package com.eodigaljido.backend.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "채팅방 멤버 요약 정보")
public record MemberSummary(
        @Schema(description = "멤버 UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String uuid,

        @Schema(description = "멤버 아이디", example = "john123")
        String userId,

        @Schema(description = "멤버 프로필 이미지 URL (없으면 null)", example = "/images/profile/default.png")
        String profileImageUrl
) {}
