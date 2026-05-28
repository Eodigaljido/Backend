package com.eodigaljido.backend.dto.group;

import com.eodigaljido.backend.domain.group.GroupPost;
import com.eodigaljido.backend.domain.user.Profile;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "모임 게시물 응답")
public record GroupPostResponse(
        @Schema(description = "게시물 UUID") String uuid,
        @Schema(description = "작성자 UUID") String authorUuid,
        @Schema(description = "작성자 아이디") String authorUserId,
        @Schema(description = "작성자 닉네임") String authorNickname,
        @Schema(description = "작성자 프로필 이미지 URL") String authorProfileImageUrl,
        @Schema(description = "게시물 내용") String content,
        @Schema(description = "이미지 URL 목록") List<String> imageUrls,
        @Schema(description = "작성 일시") LocalDateTime createdAt,
        @Schema(description = "수정 일시") LocalDateTime updatedAt
) {
    public static GroupPostResponse of(GroupPost post, Profile profile) {
        return new GroupPostResponse(
                post.getUuid(),
                post.getAuthor().getUuid(),
                post.getAuthor().getUserId(),
                profile != null ? profile.getNickname() : post.getAuthor().getUserId(),
                profile != null ? profile.getProfileImageUrl() : null,
                post.getContent(),
                post.getImageUrls() != null ? List.copyOf(post.getImageUrls()) : List.of(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
