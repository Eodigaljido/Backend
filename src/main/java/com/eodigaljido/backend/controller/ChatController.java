package com.eodigaljido.backend.controller;

import com.eodigaljido.backend.dto.chat.*;
import com.eodigaljido.backend.dto.common.ErrorResponse;
import com.eodigaljido.backend.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chats")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "채팅 API")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    @Operation(
            summary = "채팅방 생성",
            description = """
                    새 채팅방을 생성하고 멤버를 초대합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Request Body:**
                    - `memberUuids` (필수): 초대할 멤버의 UUID 목록 (본인 UUID 포함 시 자동 제외)
                    - `name` (선택): 채팅방 이름 (최대 100자, 미입력 시 멤버 닉네임 조합으로 자동 생성)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "채팅방 생성 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "초대한 UUID에 해당하는 유저가 존재하지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CreateChatRoomResponse> createRoom(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateChatRoomRequest req) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.status(201).body(chatService.createRoom(userId, req));
    }

    @GetMapping
    @Operation(
            summary = "채팅방 목록 조회",
            description = """
                    로그인한 사용자가 참여 중인 채팅방 목록을 조회합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Response:** 참여 중인 채팅방 목록 (생성 최신순)
                    - `uuid`: 채팅방 UUID
                    - `name`: 채팅방 이름 (미설정 시 멤버 닉네임 조합으로 자동 생성)
                    - `memberCount`: 전체 멤버 수
                    - `ownerUuid` / `ownerUserId`: 방장 UUID / 아이디
                    - `memberUuids` / `memberUserIds`: 채팅방 입장 순서 기준 **최대 3명**의 멤버 UUID / 아이디 목록. 전체 인원은 `memberCount` 참고
                    - `lastMessage` / `lastMessageAt`: 마지막 메시지 내용 및 전송 시각 (없으면 null)
                    - `unreadCount`: 읽지 않은 메시지 수
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "채팅방 목록 반환 (빈 배열이면 참여 중인 채팅방 없음)"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<ChatRoomResponse>> getRooms(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(chatService.getRooms(userId));
    }

    @GetMapping("/{roomUuid}")
    @Operation(
            summary = "채팅방 단건 조회",
            description = """
                    특정 채팅방의 상세 정보를 조회합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Path Variable:**
                    - `roomUuid`: 조회할 채팅방의 UUID
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "채팅방 정보 반환"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "해당 채팅방의 멤버가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ChatRoomResponse> getRoom(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String roomUuid) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(chatService.getRoom(userId, roomUuid));
    }

    @DeleteMapping("/{roomUuid}")
    @Operation(
            summary = "채팅방 삭제",
            description = """
                    채팅방을 삭제합니다. ADMIN 권한(방장)만 가능합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Path Variable:**
                    - `roomUuid`: 삭제할 채팅방의 UUID
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "채팅방 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 없음 또는 채팅방 멤버가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteRoom(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String roomUuid) {
        Long userId = Long.valueOf(userDetails.getUsername());
        chatService.deleteRoom(userId, roomUuid);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{roomUuid}/me")
    @Operation(
            summary = "채팅방 나가기",
            description = """
                    채팅방에서 나갑니다. 마지막 멤버가 나가면 채팅방이 자동 삭제됩니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Path Variable:**
                    - `roomUuid`: 나갈 채팅방의 UUID
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "채팅방 나가기 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "해당 채팅방의 멤버가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> leaveRoom(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String roomUuid) {
        Long userId = Long.valueOf(userDetails.getUsername());
        chatService.leaveRoom(userId, roomUuid);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{roomUuid}/messages")
    @Operation(
            summary = "메시지 전송",
            description = """
                    채팅방에 메시지를 전송합니다. 전송 시 WebSocket 구독자(`/topic/chat/{roomUuid}`)에게도 실시간 브로드캐스트됩니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Path Variable:**
                    - `roomUuid`: 메시지를 전송할 채팅방의 UUID

                    **Request Body:**
                    - `content` (필수): 메시지 내용 (최대 2000자)

                    **WebSocket 연결 인증:**
                    STOMP CONNECT 프레임의 `Authorization` 헤더에 `Bearer {accessToken}`을 포함해야 합니다.
                    토큰이 없거나 유효하지 않으면 연결이 즉시 거부됩니다 (401).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "메시지 전송 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "해당 채팅방의 멤버가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String roomUuid,
            @Valid @RequestBody SendMessageRequest req) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.status(201).body(chatService.sendMessage(userId, roomUuid, req));
    }

    @GetMapping("/{roomUuid}/messages")
    @Operation(
            summary = "메시지 목록 조회",
            description = """
                    채팅방의 메시지 목록을 조회합니다 (최신순, 최대 100건).

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Path Variable:**
                    - `roomUuid`: 조회할 채팅방의 UUID

                    **Query Parameters:**
                    - `beforeMessageUuid` (선택): 이 UUID 메시지보다 이전 메시지를 조회합니다. 무한 스크롤 시 마지막으로 받은 메시지의 UUID를 전달하세요. 미입력 시 최신 메시지부터 반환합니다.
                    - `limit` (선택): 조회할 메시지 수 (기본값 50, 최대 100)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메시지 목록 반환 (결과가 비어 있으면 더 이상 이전 메시지 없음)"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "해당 채팅방의 멤버가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방 또는 beforeMessageUuid에 해당하는 메시지",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<ChatMessageResponse>> getMessages(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String roomUuid,
            @RequestParam(required = false) String beforeMessageUuid,
            @RequestParam(defaultValue = "50") int limit) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(chatService.getMessages(userId, roomUuid, beforeMessageUuid, limit));
    }

    @PatchMapping("/{roomUuid}/messages/{messageUuid}")
    @Operation(
            summary = "메시지 수정",
            description = """
                    본인이 작성한 메시지를 수정합니다. 삭제된 메시지는 수정할 수 없습니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Path Variables:**
                    - `roomUuid`: 채팅방의 UUID
                    - `messageUuid`: 수정할 메시지의 UUID

                    **Request Body:**
                    - `content` (필수): 수정할 내용 (최대 2000자)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "메시지 수정 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않음 또는 삭제된 메시지",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인이 작성한 메시지가 아님 또는 채팅방 멤버가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방 또는 메시지",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ChatMessageResponse> editMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String roomUuid,
            @PathVariable String messageUuid,
            @Valid @RequestBody EditMessageRequest req) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(chatService.editMessage(userId, roomUuid, messageUuid, req));
    }

    @DeleteMapping("/{roomUuid}/messages/{messageUuid}")
    @Operation(
            summary = "메시지 삭제",
            description = """
                    메시지를 삭제합니다. 본인이 작성한 메시지 또는 ADMIN(방장)은 모든 메시지를 삭제할 수 있습니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Path Variables:**
                    - `roomUuid`: 채팅방의 UUID
                    - `messageUuid`: 삭제할 메시지의 UUID
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "메시지 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "해당 채팅방의 메시지가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "삭제 권한 없음 (본인 메시지도 아니고 ADMIN도 아님)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방 또는 메시지",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String roomUuid,
            @PathVariable String messageUuid) {
        Long userId = Long.valueOf(userDetails.getUsername());
        chatService.deleteMessage(userId, roomUuid, messageUuid);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{roomUuid}/name")
    @Operation(
            summary = "채팅방 이름 변경",
            description = """
                    채팅방 이름을 변경합니다. ADMIN 권한(방장)만 가능합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Path Variable:**
                    - `roomUuid`: 이름을 변경할 채팅방의 UUID

                    **Request Body:**
                    - `name` (선택): 새 채팅방 이름 (최대 100자, null 또는 빈 문자열 입력 시 자동 생성 이름으로 초기화)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "채팅방 이름 변경 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 없음 또는 채팅방 멤버가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ChatRoomResponse> updateRoomName(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String roomUuid,
            @Valid @RequestBody UpdateChatRoomNameRequest req) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(chatService.updateRoomName(userId, roomUuid, req));
    }

    @PostMapping("/{roomUuid}/members")
    @Operation(
            summary = "채팅방 멤버 초대",
            description = """
                    기존 채팅방에 새로운 유저를 초대합니다. 채팅방 멤버라면 누구든 초대할 수 있습니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Path Variable:**
                    - `roomUuid`: 초대할 채팅방의 UUID

                    **Request Body:**
                    - `userId` (필수): 초대할 유저의 아이디
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "멤버 초대 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "해당 채팅방의 멤버가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방 또는 유저",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 채팅방에 참여 중인 유저",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ChatRoomResponse> inviteMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String roomUuid,
            @Valid @RequestBody InviteMemberRequest req) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(chatService.inviteMember(userId, roomUuid, req.userId()));
    }

    @DeleteMapping("/{roomUuid}/members/{targetUuid}")
    @Operation(
            summary = "멤버 강퇴",
            description = """
                    채팅방에서 특정 멤버를 강퇴합니다. ADMIN 권한(방장)만 가능합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Path Variables:**
                    - `roomUuid`: 채팅방의 UUID
                    - `targetUuid`: 강퇴할 멤버의 UUID
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "멤버 강퇴 성공"),
            @ApiResponse(responseCode = "400", description = "자기 자신은 강퇴 불가",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 없음 또는 채팅방 멤버가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방, 유저 또는 채팅방 멤버가 아닌 대상",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> kickMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String roomUuid,
            @PathVariable String targetUuid) {
        Long userId = Long.valueOf(userDetails.getUsername());
        chatService.kickMember(userId, roomUuid, targetUuid);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{roomUuid}/read")
    @Operation(
            summary = "읽음 처리",
            description = """
                    채팅방의 메시지를 읽음 처리합니다. 현재 시각을 마지막 읽은 시각으로 업데이트합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Path Variable:**
                    - `roomUuid`: 읽음 처리할 채팅방의 UUID
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "읽음 처리 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "해당 채팅방의 멤버가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String roomUuid) {
        Long userId = Long.valueOf(userDetails.getUsername());
        chatService.markAsRead(userId, roomUuid);
        return ResponseEntity.noContent().build();
    }
}
