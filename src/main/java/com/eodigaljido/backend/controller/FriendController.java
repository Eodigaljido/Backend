package com.eodigaljido.backend.controller;

import com.eodigaljido.backend.dto.common.ErrorResponse;
import com.eodigaljido.backend.dto.friend.FriendRequestDto;
import com.eodigaljido.backend.dto.friend.FriendRequestResponse;
import com.eodigaljido.backend.dto.friend.FriendRespondDto;
import com.eodigaljido.backend.dto.friend.FriendResponse;
import com.eodigaljido.backend.dto.friend.RecentFriendResponse;
import com.eodigaljido.backend.service.FriendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
@Tag(name = "Friend", description = "친구 API")
public class FriendController {

    private final FriendService friendService;

    @GetMapping("/recent")
    @Operation(
            summary = "최근 연락한 친구 5명 조회",
            description = """
                    가장 최근에 채팅을 나눈 친구를 최대 5명 반환합니다.
                    채팅 이력이 없는 친구는 포함되지 않습니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "최근 연락한 친구 목록 반환 (최대 5명, 빈 배열이면 없음)"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<RecentFriendResponse>> getRecentFriends(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(friendService.getRecentFriends(Long.parseLong(userDetails.getUsername())));
    }

    @GetMapping
    @Operation(
            summary = "전체 친구 목록 조회",
            description = """
                    로그인한 사용자의 수락된 친구 목록을 조회합니다.
                    닉네임 기준으로 한글 → 영어 → 숫자 → 특수기호 순으로 정렬되며,
                    각 그룹 내에서는 가나다/abc/123 순으로 정렬됩니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "친구 목록 반환 (빈 배열이면 친구 없음)"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<FriendResponse>> getFriends(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(friendService.getFriends(Long.parseLong(userDetails.getUsername())));
    }

    @PostMapping("/requests")
    @Operation(
            summary = "친구 요청 전송",
            description = """
                    상대방의 UUID로 친구 요청을 전송합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Request Body:**
                    - `targetUuid` (필수): 친구 요청을 보낼 상대방의 UUID
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "친구 요청 전송 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않음 또는 자기 자신에게 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 요청 중이거나 이미 친구 관계 또는 요청 불가 상태",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> sendRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody FriendRequestDto request) {
        friendService.sendRequest(Long.parseLong(userDetails.getUsername()), request.targetUuid());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{friendId}")
    @Operation(
            summary = "친구 삭제",
            description = """
                    수락된 친구 관계를 삭제합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Path Variable:**
                    - `friendId`: 삭제할 친구 관계 ID (친구 목록 조회 응답의 `friendId` 값)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "친구 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "친구 관계를 찾을 수 없음 (본인 관계가 아니거나 미수락 상태)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteFriend(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long friendId) {
        friendService.deleteFriend(Long.parseLong(userDetails.getUsername()), friendId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/requests/{requestId}")
    @Operation(
            summary = "친구 요청 수락/거절",
            description = """
                    받은 친구 요청을 수락하거나 거절합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Path Variable:**
                    - `requestId`: 처리할 친구 요청 ID (요청 목록 조회 응답의 `requestId` 값)

                    **Request Body:**
                    - `accept` (필수): `true` = 수락, `false` = 거절
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "수락 또는 거절 처리 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인에게 온 요청이 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 친구 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 수락 또는 거절 처리된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> respondToRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long requestId,
            @Valid @RequestBody FriendRespondDto request) {
        friendService.respondToRequest(Long.parseLong(userDetails.getUsername()), requestId, request.accept());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/requests")
    @Operation(
            summary = "보낸/받은 친구 요청 목록 조회",
            description = """
                    대기 중인 보낸 요청과 받은 요청 목록을 모두 반환합니다.
                    각 항목의 `direction` 필드로 구분합니다.

                    - `SENT`: 내가 보낸 요청
                    - `RECEIVED`: 내가 받은 요청

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "대기 중인 요청 목록 반환 (빈 배열이면 요청 없음)"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<FriendRequestResponse>> getPendingRequests(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(friendService.getPendingRequests(Long.parseLong(userDetails.getUsername())));
    }
}
