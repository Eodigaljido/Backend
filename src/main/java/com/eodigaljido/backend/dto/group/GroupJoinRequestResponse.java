package com.eodigaljido.backend.dto.group;

import com.eodigaljido.backend.domain.group.GroupJoinRequest;
import com.eodigaljido.backend.domain.user.Profile;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "모임 가입 요청 응답")
public record GroupJoinRequestResponse(
        @Schema(description = "요청 ID") Long requestId,
        @Schema(description = "요청자 UUID") String requesterUuid,
        @Schema(description = "요청자 아이디") String requesterUserId,
        @Schema(description = "요청자 닉네임") String requesterNickname,
        @Schema(description = "요청자 프로필 이미지 URL") String requesterProfileImageUrl,
        @Schema(description = "요청 일시") LocalDateTime createdAt
) {
    public static GroupJoinRequestResponse of(GroupJoinRequest request, Profile profile) {
        return new GroupJoinRequestResponse(
                request.getId(),
                request.getRequester().getUuid(),
                request.getRequester().getUserId(),
                profile != null ? profile.getNickname() : request.getRequester().getUserId(),
                profile != null ? profile.getProfileImageUrl() : null,
                request.getCreatedAt()
        );
    }
}
