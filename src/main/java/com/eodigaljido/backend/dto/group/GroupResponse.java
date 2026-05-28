package com.eodigaljido.backend.dto.group;

import com.eodigaljido.backend.domain.group.Group;
import com.eodigaljido.backend.domain.group.Group.GroupType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "모임 응답")
public record GroupResponse(
        @Schema(description = "모임 UUID") String uuid,
        @Schema(description = "모임 이름") String name,
        @Schema(description = "모임 소개") String description,
        @Schema(description = "프로필 이미지 URL") String profileImageUrl,
        @Schema(description = "공개 여부") GroupType type,
        @Schema(description = "가입 승인 필요 여부") boolean requiresApproval,
        @Schema(description = "조회수") int viewCount,
        @Schema(description = "멤버 수") long memberCount,
        @Schema(description = "내가 멤버인지 여부") boolean joinedByMe,
        @Schema(description = "방장 UUID") String adminUuid,
        @Schema(description = "방장 아이디") String adminUserId
) {
    public static GroupResponse of(Group group, long memberCount, boolean joinedByMe) {
        return new GroupResponse(
                group.getUuid(),
                group.getName(),
                group.getDescription(),
                group.getProfileImageUrl(),
                group.getType(),
                group.isRequiresApproval(),
                group.getViewCount(),
                memberCount,
                joinedByMe,
                group.getCreatedBy().getUuid(),
                group.getCreatedBy().getUserId()
        );
    }
}
