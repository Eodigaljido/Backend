package com.eodigaljido.backend.dto.following;

import com.eodigaljido.backend.domain.following.FollowingNews;
import com.eodigaljido.backend.domain.following.FollowingNewsActionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "팔로잉 소식 아이템")
public record FollowingNewsItemResponse(
        @Schema(description = "소식 고유 ID", example = "news_101")
        String id,

        @Schema(description = "사용자 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        String userId,

        @Schema(description = "유저 닉네임", example = "산책러버")
        String nickname,

        @Schema(description = "액션 타입", example = "COURSE_PUBLISHED")
        FollowingNewsActionType actionType,

        @Schema(description = "화면 노출용 액션 문구", example = "새 코스를 공개했어요")
        String actionText,

        @Schema(description = "코스 UUID", nullable = true, example = "550e8400-e29b-41d4-a716-446655440000")
        String courseId,

        @Schema(description = "코스명", nullable = true, example = "홍대 야간 산책 코스")
        String courseName,

        @Schema(description = "생성 시각", example = "2026-05-07T10:02:00")
        LocalDateTime createdAt,

        @Schema(description = "상대 시간 문자열", example = "9분 전")
        String timeAgo
) {
    public static FollowingNewsItemResponse from(FollowingNews news, String nickname, String timeAgo) {
        return new FollowingNewsItemResponse(
                encodeId(news.getId()),
                news.getActor().getUuid(),
                nickname,
                news.getActionType(),
                news.getActionType().getActionText(),
                news.getCourseId(),
                news.getCourseName(),
                news.getCreatedAt(),
                timeAgo
        );
    }

    public static String encodeId(Long id) {
        return "news_" + id;
    }
}
