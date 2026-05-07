package com.eodigaljido.backend.controller;

import com.eodigaljido.backend.domain.following.FollowingNewsActionType;
import com.eodigaljido.backend.dto.common.ErrorResponse;
import com.eodigaljido.backend.dto.following.FollowingNewsFeedResponse;
import com.eodigaljido.backend.service.FollowingNewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/following/news")
@RequiredArgsConstructor
@Tag(name = "Following News", description = "팔로잉 소식 API")
public class FollowingNewsController {

    private final FollowingNewsService followingNewsService;

    @GetMapping
    @Operation(
            summary = "팔로잉 소식 조회",
            description = """
                    홈 화면의 `팔로잉 소식` 섹션에 노출할 최신 소식을 반환합니다.
                    로그인 사용자의 수락된 친구가 만든 소식만 최신순(`createdAt DESC`)으로 조회됩니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Query Parameter:**
                    - `limit` (선택): 반환 개수, 기본 3, 최대 50
                    - `cursor` (선택): 다음 페이지 조회용 커서. 응답의 `nextCursor` 값을 그대로 전달
                    - `type` (선택): 액션 타입 필터
                      - `COURSE_PUBLISHED`
                      - `COURSE_COMPLETED`
                      - `COURSE_LIKED`
                      - `COURSE_SAVED`
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "팔로잉 소식 목록 반환",
                    content = @Content(schema = @Schema(implementation = FollowingNewsFeedResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "cursor 또는 type 값이 올바르지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<FollowingNewsFeedResponse> getFollowingNews(
            @Parameter(description = "반환할 소식 수 (기본 3, 최대 50)", example = "3")
            @RequestParam(defaultValue = "3") int limit,

            @Parameter(description = "다음 페이지 조회용 커서", example = "news_100")
            @RequestParam(required = false) String cursor,

            @Parameter(description = "액션 타입 필터", example = "COURSE_PUBLISHED")
            @RequestParam(required = false) FollowingNewsActionType type,

            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(followingNewsService.getNews(userId, limit, cursor, type));
    }

    @PatchMapping("/{id}/read")
    @Operation(
            summary = "팔로잉 소식 단건 읽음 처리",
            description = """
                    특정 팔로잉 소식을 읽음 처리합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Path Variable:**
                    - `id`: 읽음 처리할 소식 ID (`GET /api/following/news` 응답의 `id`)
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "읽음 처리 성공"),
            @ApiResponse(responseCode = "400", description = "소식 ID 형식이 올바르지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "조회 가능한 소식을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "소식 ID", example = "news_101")
            @PathVariable String id) {
        Long userId = Long.parseLong(userDetails.getUsername());
        followingNewsService.markAsRead(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    @Operation(
            summary = "팔로잉 소식 일괄 읽음 처리",
            description = """
                    현재 로그인 사용자가 조회 가능한 모든 팔로잉 소식을 읽음 처리합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "일괄 읽음 처리 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        followingNewsService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }
}
