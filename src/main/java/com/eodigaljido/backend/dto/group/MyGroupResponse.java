package com.eodigaljido.backend.dto.group;

import com.eodigaljido.backend.domain.group.Group;
import com.eodigaljido.backend.domain.group.Group.GroupType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "내 모임 응답")
public record MyGroupResponse(
        @Schema(description = "모임 UUID") String uuid,
        @Schema(description = "모임 이름") String name,
        @Schema(description = "모임 소개") String description,
        @Schema(description = "프로필 이미지 URL") String profileImageUrl,
        @Schema(description = "공개 여부") GroupType type,
        @Schema(description = "멤버 수") long memberCount,
        @Schema(description = "방장 UUID") String adminUuid,
        @Schema(description = "방장 아이디") String adminUserId,
        @Schema(description = "멤버 목록") List<GroupMemberResponse> members
) {
    public static MyGroupResponse of(Group group, long memberCount, List<GroupMemberResponse> members) {
        return new MyGroupResponse(
                group.getUuid(),
                group.getName(),
                group.getDescription(),
                group.getProfileImageUrl(),
                group.getType(),
                memberCount,
                group.getCreatedBy().getUuid(),
                group.getCreatedBy().getUserId(),
                members
        );
    }
}
