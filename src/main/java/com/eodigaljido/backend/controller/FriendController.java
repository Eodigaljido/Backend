package com.eodigaljido.backend.controller;

import com.eodigaljido.backend.dto.friend.FriendRequestDto;
import com.eodigaljido.backend.dto.friend.FriendRequestResponse;
import com.eodigaljido.backend.dto.friend.FriendRespondDto;
import com.eodigaljido.backend.dto.friend.FriendResponse;
import com.eodigaljido.backend.service.FriendService;
import io.swagger.v3.oas.annotations.Operation;
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

    @GetMapping
    @Operation(
            summary = "친구 목록 조회",
            description = """
                    로그인한 사용자의 친구 목록을 조회합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)
                    """
    )
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
                    친구 관계를 삭제합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Path Variable:**
                    - `friendId`: 삭제할 친구 관계 ID
                    """
    )
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
                    - `requestId`: 처리할 친구 요청 ID

                    **Request Body:**
                    - `accept` (필수): `true` = 수락, `false` = 거절
                    """
    )
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
                    각 항목의 `direction` 필드로 구분합니다: `SENT` (보낸 요청), `RECEIVED` (받은 요청)

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)
                    """
    )
    public ResponseEntity<List<FriendRequestResponse>> getPendingRequests(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(friendService.getPendingRequests(Long.parseLong(userDetails.getUsername())));
    }
}
