package com.eodigaljido.backend.dto.course;

import com.eodigaljido.backend.domain.chat.ChatRoomMember;
import com.eodigaljido.backend.domain.user.Profile;
import com.eodigaljido.backend.domain.user.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "공동 루트 멤버 응답")
public record CollaborativeMemberResponse(
        @Schema(description = "사용자 PK", example = "1")
        Long userId,

        @Schema(description = "사용자 UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String uuid,

        @Schema(description = "로그인 아이디", example = "jane456")
        String accountId,

        @Schema(description = "닉네임")
        String nickname,

        @Schema(description = "프로필 이미지 URL")
        String profileImageUrl,

        @Schema(description = "역할", example = "ADMIN")
        String role,

        @Schema(description = "참여 시각")
        LocalDateTime joinedAt
) {
    public static CollaborativeMemberResponse ofOwner(User owner, Profile profile) {
        return new CollaborativeMemberResponse(
                owner.getId(),
                owner.getUuid(),
                owner.getUserId(),
                profile != null ? profile.getNickname() : null,
                profile != null ? profile.getProfileImageUrl() : null,
                "ADMIN",
                null
        );
    }

    public static CollaborativeMemberResponse of(ChatRoomMember member, Profile profile) {
        User user = member.getUser();
        return new CollaborativeMemberResponse(
                user.getId(),
                user.getUuid(),
                user.getUserId(),
                profile != null ? profile.getNickname() : null,
                profile != null ? profile.getProfileImageUrl() : null,
                member.getRole().name(),
                member.getCreatedAt()
        );
    }
}
