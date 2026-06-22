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
                    그룹 채팅방에 속한 루트 기록 목록을 반환합니다.

                    **요청 조건**
                    - `chatRoomUuid`: GROUP 타입 채팅방 UUID
                    - 해당 채팅방 멤버만 조회 가능합니다.

                    **반환 조건**
                    - 해당 그룹 내 루트 중, 실시간 공동 편집 세션을 위한 전용 채팅방(ROUTE 타입)이
                      생성된 적 있는 루트만 목록에 포함됩니다.
                    - 일반 채팅만 나눈 루트(전용 채팅방이 없는 루트)는 목록에 표시되지 않습니다.

                    **응답 필드**
                    | 필드 | 설명 |
                    |------|------|
                    | `courseUuid` | 루트 UUID |
                    | `routeChatRoomUuid` | 루트 전용 채팅방 UUID |
                    | `name` | 루트 기록방 이름 (루트 제목) |
                    | `participantCount` | 실시간 공동 편집에 참여한 전체 인원 수 |
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
            @Parameter(description = "그룹 채팅방 UUID (GROUP 타입)", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam String chatRoomUuid,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(routeHistoryService.getRouteHistories(userId, chatRoomUuid));
    }

    // ──────────────────────────────────────────────────────────
    // 루트 기록 상세 피드
    // ──────────────────────────────────────────────────────────

    @GetMapping("/{courseId}/feed")
    @Operation(
            summary = "루트 기록 상세 피드 조회",
            description = """
                    특정 루트의 실시간 공동 편집 세션 중 발생한 채팅 메시지와 루트 수정 이벤트를
                    발생 시각 오름차순으로 반환합니다.

                    이벤트는 발생 시점에 불변 로그로 저장되므로, 원본 채팅 메시지를 이후에
                    수정·삭제하더라도 기록 자체는 유지됩니다.

                    **`type` 값에 따른 응답 필드 사용 방법**

                    | `type` | `action` | 설명 | 유효 필드 |
                    |--------|----------|------|-----------|
                    | `CHAT` | `CHAT_SENDED` | 채팅 메시지 전송 | `content`(전송 내용), `editDescription`, `actorNickname`, `actorProfileImageUrl` |
                    | `CHAT` | `CHAT_EDITED` | 채팅 메시지 수정 | `content`(수정 후 내용), `editDescription`, `actorNickname`, `actorProfileImageUrl` |
                    | `CHAT` | `CHAT_DELETED` | 채팅 메시지 삭제 | `content`(삭제 직전 내용), `editDescription`, `actorNickname`, `actorProfileImageUrl` |
                    | `COURSE` | `ROUTE_UPDATED` | 그 외 루트 편집(위 항목에 해당 없음) | `editDescription`, `actorNickname`, `actorProfileImageUrl` (`content`는 null) |
                    | `COURSE` | `TITLE_CHANGED` | 루트 제목 변경 | 〃 |
                    | `COURSE` | `STOP_ADDED` | 경유지 추가 | 〃 |
                    | `COURSE` | `STOP_REMOVED` | 경유지 삭제 | 〃 |
                    | `COURSE` | `LEG_UPDATED` | 이동 구간 수정 | 〃 |
                    | `COURSE` | `EDITING_COMPLETED` | 공동 편집 완료 처리 | 〃 |
                    | `COURSE` | `EDITING_RESUMED` | 공동 편집 재개 처리 | 〃 |

                    **`editDescription` 예시**
                    - `ROUTE_UPDATED` → "홍길동님이 루트를 수정했습니다"
                    - `TITLE_CHANGED` → "홍길동님이 루트 이름을 변경했습니다"
                    - `STOP_ADDED` → "홍길동님이 경유지를 추가했습니다"
                    - `STOP_REMOVED` → "홍길동님이 경유지를 삭제했습니다"
                    - `LEG_UPDATED` → "홍길동님이 이동 구간을 수정했습니다"
                    - `EDITING_COMPLETED` → "홍길동님이 편집을 완료했습니다"
                    - `EDITING_RESUMED` → "홍길동님이 편집을 재개했습니다"
                    - `CHAT_SENDED` → "홍길동님이 메시지를 보냈습니다"
                    - `CHAT_EDITED` → "홍길동님이 메시지를 수정했습니다"
                    - `CHAT_DELETED` → "홍길동님이 메시지를 삭제했습니다"

                    **조회 권한:** 루트의 CourseMember이거나, 루트 전용 채팅방 멤버이거나,
                    루트가 속한 그룹의 채팅방 멤버인 경우 조회 가능합니다.

                    이 API는 조회 전용입니다. 루트를 추가로 수정하려면 실시간 공동 편집
                    세션에 재진입해야 합니다.
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "루트 기록 상세 피드 반환",
                    content = @Content(schema = @Schema(implementation = RouteHistoryFeedResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음 (CourseMember, 루트 채팅방 멤버, 그룹 채팅방 멤버 중 하나여야 함)",
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
        int clampedSize = Math.min(Math.max(size, 1), 50);
        return ResponseEntity.ok(routeHistoryService.getRouteHistoryFeed(userId, courseId, page, clampedSize));
    }
}
