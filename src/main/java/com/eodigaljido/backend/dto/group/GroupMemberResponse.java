package com.eodigaljido.backend.dto.group;

import com.eodigaljido.backend.domain.group.GroupMember;
import com.eodigaljido.backend.domain.user.Profile;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "모임 멤버 응답")
public record GroupMemberResponse(
        @Schema(description = "사용자 UUID") String userUuid,
        @Schema(description = "사용자 아이디") String userId,
        @Schema(description = "닉네임") String nickname,
        @Schema(description = "프로필 이미지 URL") String profileImageUrl,
        @Schema(description = "역할: ADMIN | MEMBER") GroupMember.MemberRole role,
        @Schema(description = "가입 일시") LocalDateTime joinedAt
) {
    public static GroupMemberResponse of(GroupMember member, Profile profile) {
        return new GroupMemberResponse(
                member.getUser().getUuid(),
                member.getUser().getUserId(),
                profile != null ? profile.getNickname() : member.getUser().getUserId(),
                profile != null ? profile.getProfileImageUrl() : null,
                member.getRole(),
                member.getJoinedAt()
        );
    }
}
