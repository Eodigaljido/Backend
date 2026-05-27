package com.eodigaljido.backend.dto.course;

import com.eodigaljido.backend.domain.route.RouteJoinRequest;
import com.eodigaljido.backend.domain.user.Profile;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "공동 루트 입장 요청 정보")
public record JoinRequestResponse(
        @Schema(description = "입장 요청 ID", example = "1")
        Long requestId,

        @Schema(description = "요청자 UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String requesterUuid,

        @Schema(description = "요청자 로그인 아이디", example = "jane456")
        String requesterAccountId,

        @Schema(description = "요청자 닉네임", example = "제인")
        String requesterNickname,

        @Schema(description = "요청자 프로필 이미지 URL")
        String requesterProfileImageUrl,

        @Schema(description = "요청 상태", example = "PENDING", allowableValues = {"PENDING", "APPROVED", "REJECTED"})
        String status,

        @Schema(description = "요청 생성 시각", example = "2026-05-27T10:00:00")
        LocalDateTime createdAt,

        @Schema(description = "처리 시각 (미처리 시 null)", example = "null")
        LocalDateTime processedAt
) {
    public static JoinRequestResponse of(RouteJoinRequest req, Profile profile) {
        return new JoinRequestResponse(
                req.getId(),
                req.getRequester().getUuid(),
                req.getRequester().getUserId(),
                profile != null ? profile.getNickname() : null,
                profile != null ? profile.getProfileImageUrl() : null,
                req.getStatus().name(),
                req.getCreatedAt(),
                req.getProcessedAt()
        );
    }
}
