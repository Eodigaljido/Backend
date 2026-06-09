package com.eodigaljido.backend.controller;

import com.eodigaljido.backend.dto.common.ErrorResponse;
import com.eodigaljido.backend.dto.routehistory.RouteHistoryFeedResponse;
import com.eodigaljido.backend.dto.routehistory.RouteHistoryItemResponse;
import com.eodigaljido.backend.service.RouteHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/route-history")
@RequiredArgsConstructor
@Tag(name = "RouteHistory", description = "루트 기록 보기 API")
public class RouteHistoryController {

    private final RouteHistoryService routeHistoryService;

    // ──────────────────────────────────────────────────────────
    // 루트 기록 목록 조회
    // ──────────────────────────────────────────────────────────

    @GetMapping
    @Operation(
            summary = "루트 기록 목록 조회",
            description = """
                    채팅방(그룹)에 속한 루트 기록 목록을 반환합니다.

                    - `chatRoomUuid`: 그룹 채팅방 UUID (GROUP 타입)
                    - 해당 채팅방에 속한 그룹의 모든 루트(전용 채팅방이 있는 루트만)를 반환합니다.
                    - 루트 기록방 이름, 루트 UUID, 채팅방 UUID, 참가 인원 수를 포함합니다.
                    - 채팅방 멤버인 경우에만 조회 가능합니다.
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "루트 기록 목록 반환",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = RouteHistoryItemResponse.class)))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "채팅방 멤버가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<RouteHistoryItemResponse>> getRouteHistories(
            @Parameter(description = "그룹 채팅방 UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam String chatRoomUuid,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(routeHistoryService.getRouteHistories(userId, chatRoomUuid));
    }

    // ──────────────────────────────────────────────────────────
    // 루트 기록 피드 (상세)
    // ──────────────────────────────────────────────────────────

    @GetMapping("/{courseId}/feed")
    @Operation(
            summary = "루트 기록 피드 조회",
            description = """
                    특정 루트의 기록 피드를 시간 순서대로 반환합니다.

                    피드에는 두 종류의 항목이 포함됩니다:
                    - **CHAT**: 루트 편집 세션 중 나눈 채팅 메시지
                    - **EDIT**: 루트 수정 이벤트 (예: "홍길동님이 경유지를 추가했습니다")

                    각 항목에는 작성자 프로필 이미지, 닉네임, 발생 시각이 포함됩니다.

                    **조회 권한:** CourseMember이거나, 루트 전용 채팅방 멤버이거나, 그룹 채팅방 멤버인 경우 조회 가능합니다.

                    **주의:** 이 API는 조회 전용입니다. 루트 수정은 공동 편집 세션에서만 가능합니다.
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "루트 기록 피드 반환",
                    content = @Content(schema = @Schema(implementation = RouteHistoryFeedResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "루트를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RouteHistoryFeedResponse> getRouteHistoryFeed(
            @Parameter(description = "루트 UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String courseId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (최대 50)", example = "30")
            @RequestParam(defaultValue = "30") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        int clampedSize = Math.min(size, 50);
        return ResponseEntity.ok(routeHistoryService.getRouteHistoryFeed(userId, courseId, page, clampedSize));
    }
}
