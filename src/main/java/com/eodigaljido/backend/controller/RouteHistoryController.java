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
                    부모 채팅방 안에 종속된 루트 기록방(ROUTE 타입 자식 채팅방) 목록을 반환합니다.

                    **요청 조건**
                    - `chatRoomUuid`: 부모로 사용 중인 채팅방 UUID (DIRECT/GROUP 등 타입 무관)
                    - 해당 채팅방 멤버만 조회 가능합니다.

                    **반환 조건**
                    - 이 채팅방을 부모로 하는 루트 기록방(ROUTE 타입 자식 채팅방)에 연결된 루트만 포함됩니다.
                    - 공동편집 친구 초대(addCourseMember)로 만들어진, 부모가 없는 독립 기록방은
                      이 목록에 나타나지 않습니다(해당 루트 화면에서 별도로 조회해야 함).

                    **응답 필드**
                    | 필드 | 설명 |
                    |------|------|
                    | `courseUuid` | 루트 UUID |
                    | `routeChatRoomUuid` | 루트 기록방(자식 채팅방) UUID |
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
            @Parameter(description = "부모 채팅방 UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
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
                    특정 루트에 연결된 채팅방(`chat_messages`)에서 발생한 채팅 메시지와 루트 수정 이벤트를
                    발생 시각 오름차순으로 반환합니다. 루트에 연결된 채팅방이 없으면 빈 목록을 반환합니다.

                    채팅 메시지를 실제로 수정/삭제하면 그 메시지 row 자체가 바뀌거나 사라지므로,
                    이 피드에 보이는 내용도 함께 바뀌거나 사라집니다(불변 로그가 아닙니다).

                    **`type` 값에 따른 응답 필드 사용 방법**

                    | `type` | `action` | 설명 | 유효 필드 |
                    |--------|----------|------|-----------|
                    | `CHAT` | `CHAT_SENDED` | 일반 채팅 메시지 | `content`(메시지 내용), `editDescription`, `actorNickname`, `actorProfileImageUrl` |
                    | `COURSE` | `ROUTE_UPDATED` | 그 외 루트 편집(아래 항목에 해당 없음) | `editDescription`, `actorNickname`, `actorProfileImageUrl`, `editDetails` (`content`는 null) |
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

                    **`editDetails` (구체적 변경 내용, JSON 문자열, 없으면 null)**
                    - `TITLE_CHANGED` → `{"before":"이전 제목","after":"새 제목"}`
                    - `STOP_ADDED` / `STOP_REMOVED` → `{"stopNames":["신세계백화점"]}`
                    - `LEG_UPDATED` → `{"previousLegCount":2,"newLegCount":3}`
                    - 그 외 액션은 `editDetails`가 항상 null입니다.

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
