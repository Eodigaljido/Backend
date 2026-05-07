package com.eodigaljido.backend.dto.following;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "팔로잉 소식 목록 응답")
public record FollowingNewsFeedResponse(
        @ArraySchema(schema = @Schema(implementation = FollowingNewsItemResponse.class))
        List<FollowingNewsItemResponse> items,

        @Schema(description = "다음 조회 커서. 더 이상 없으면 null", nullable = true, example = "news_100")
        String nextCursor
) {}
