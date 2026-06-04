package com.eodigaljido.backend.dto.course;

import com.eodigaljido.backend.domain.route.CourseMember;
import com.eodigaljido.backend.domain.user.Profile;
import com.eodigaljido.backend.domain.user.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "공동 루트 멤버 응답")
public record CollaborativeMemberResponse(
        @Schema(description = "사용자 PK")
        Long userId,

        @Schema(description = "사용자 UUID")
        String uuid,

        @Schema(description = "로그인 ID")
        String accountId,

        @Schema(description = "닉네임")
        String nickname,

        @Schema(description = "프로필 이미지 URL")
        String profileImageUrl,

        @Schema(description = "역할")
        String role,

        @Schema(description = "온라인 여부")
        boolean online,

        @Schema(description = "마지막 확인 시각")
        LocalDateTime lastSeenAt,

        @Schema(description = "참여 시각")
        LocalDateTime joinedAt
) {
    public static CollaborativeMemberResponse of(CourseMember member, Profile profile) {
        User user = member.getUser();
        return new CollaborativeMemberResponse(
                user.getId(),
                user.getUuid(),
                user.getUserId(),
                profile != null ? profile.getNickname() : null,
                profile != null ? profile.getProfileImageUrl() : null,
                member.getRole().name(),
                false,
                member.getLastSeenAt(),
                member.getCreatedAt()
        );
    }
}
