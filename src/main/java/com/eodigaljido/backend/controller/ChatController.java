package com.eodigaljido.backend.controller;

import com.eodigaljido.backend.dto.chat.ChatMessageResponse;
import com.eodigaljido.backend.dto.chat.ChatRoomResponse;
import com.eodigaljido.backend.dto.chat.CreateChatRoomResponse;
import com.eodigaljido.backend.dto.chat.EditMessageRequest;
import com.eodigaljido.backend.dto.chat.InviteMemberRequest;
import com.eodigaljido.backend.dto.chat.SendMessageRequest;
import com.eodigaljido.backend.dto.chat.ShareRouteRequest;
import com.eodigaljido.backend.dto.chat.UpdateChatRoomNameRequest;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/chats")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "채팅 API")
public class ChatController {

    private final ChatService chatService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "채팅방 생성",
            description = """
                    새 채팅방을 생성하고 멤버를 초대합니다. `multipart/form-data`로 요청합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Request (multipart/form-data):**
                    - `memberUuids` (필수): 초대할 멤버의 UUID 목록. 여러 값을 같은 키로 반복 전송 (본인 UUID 포함 시 자동 제외)
                    - `name` (선택): 채팅방 이름 (최대 100자, 미입력 시 멤버 닉네임 조합으로 자동 생성)
                    - `image` (선택): 그룹 채팅방 프로필 이미지 (JPEG, PNG, GIF, WebP, 최대 10MB). 미입력 시 5개 기본 이미지 중 랜덤 할당. 1:1 채팅방은 무시됨

                    **Response:**
                    - `uuid`: 채팅방 UUID
                    - `name`: 채팅방 이름
                    - `profileImageUrl`: 프로필 이미지 URL (그룹 채팅방은 채팅방 이미지, 1:1 채팅방은 null)
                    - `memberCount` / `ownerUuid` / `ownerUserId` / `memberUuids` / `memberUserIds`: 멤버 정보
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "채팅방 생성 성공"),
            @ApiResponse(responseCode = "400", description = "멤버 UUID 누락 / 이미지 형식 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "초대한 UUID에 해당하는 유저가 존재하지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CreateChatRoomResponse> createRoom(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam List<String> memberUuids,
            @RequestParam(required = false) String name,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.status(201).body(chatService.createRoom(userId, memberUuids, name, image));
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
                    - `profileImageUrl`: 프로필 이미지 URL (그룹 채팅방은 채팅방 이미지, 1:1 채팅방은 상대방 프로필 이미지, 없으면 null)
                    - `memberCount`: 전체 멤버 수
                    - `ownerUuid` / `ownerUserId`: 방장 UUID / 아이디
                    - `memberUuids` / `memberUserIds`: 입장 순서 기준 **최대 3명**의 멤버 UUID / 아이디 목록. 전체 인원은 `memberCount` 참고
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

                    **Response:**
                    - `uuid`: 채팅방 UUID
                    - `name`: 채팅방 이름 (미설정 시 멤버 닉네임 조합으로 자동 생성)
                    - `profileImageUrl`: 프로필 이미지 URL (그룹 채팅방은 채팅방 이미지, 1:1 채팅방은 상대방 프로필 이미지, 없으면 null)
                    - `memberCount`: 전체 멤버 수
                    - `ownerUuid` / `ownerUserId`: 방장 UUID / 아이디
                    - `memberUuids` / `memberUserIds`: 입장 순서 기준 **최대 3명**의 멤버 UUID / 아이디 목록
                    - `lastMessage` / `lastMessageAt`: 마지막 메시지 내용 및 전송 시각 (없으면 null)
                    - `unreadCount`: 읽지 않은 메시지 수
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
            summary = "텍스트 메시지 전송",
            description = """
                    채팅방에 텍스트 메시지를 전송합니다. 전송 시 WebSocket 구독자(`/topic/chat/{roomUuid}`)에게도 실시간 브로드캐스트됩니다.
                    루트 공유는 `POST /chats/{roomUuid}/route` 를 사용하세요.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Request Body:**
                    - `content` (필수): 메시지 내용 (최대 2000자)
                    - `mentionedUserUuids` (선택): @멘션할 유저 UUID 목록

                    **Response:**
                    - `uuid`: 메시지 UUID
                    - `senderUuid` / `senderNickname` / `senderProfileImageUrl`: 발신자 정보
                    - `messageType`: 메시지 타입 (`TEXT`)
                    - `content`: 메시지 내용
                    - `createdAt` / `editedAt`: 전송·수정 시각

                    **WebSocket 실시간 수신 (STOMP):**
                    - 연결 엔드포인트: `ws://{host}/ws/chat` (SockJS 지원)
                    - CONNECT 프레임 헤더: `Authorization: Bearer {accessToken}`

                    **메시지 이벤트** (구독: `/topic/chat/{roomUuid}`)
                    ```json
                    { "eventType": "MESSAGE_CREATED" | "MESSAGE_EDITED" | "MESSAGE_DELETED",
                      "payload": { ChatMessageResponse } }
                    ```
                    - WebSocket으로 직접 텍스트 전송: destination `/app/chat/{roomUuid}`
                    - 이미지 전송은 `POST /chats/{roomUuid}/images` REST API 사용

                    **타이핑 인디케이터** (구독: `/topic/chat/{roomUuid}/typing`)
                    ```json
                    { "senderUuid": "...", "senderNickname": "홍길동", "isTyping": true }
                    ```
                    - 타이핑 상태 전송: destination `/app/chat/{roomUuid}/typing`
                    - body: `{ "isTyping": true }` (시작) / `{ "isTyping": false }` (중지)
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

                    **Query Parameters:**
                    - `beforeMessageUuid` (선택): 이 UUID 메시지보다 이전 메시지를 조회합니다. 무한 스크롤 시 마지막으로 받은 메시지의 UUID를 전달하세요. 미입력 시 최신 메시지부터 반환합니다.
                    - `limit` (선택): 조회할 메시지 수 (기본값 50, 최대 100)

                    **Response (각 메시지):**
                    - `messageType`: 메시지 타입 (`TEXT` | `IMAGE` | `ROUTE`)
                    - `content`: 메시지 내용 (삭제된 경우 null, IMAGE는 null, ROUTE는 루트 제목)
                    - `attachmentUrl`: 이미지 URL (messageType=IMAGE 일 때만, 그 외 null)
                    - `routeUuid` / `routeTitle` / `routeThumbnailUrl`: 루트 공유 메시지일 때만 값, 그 외 null
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

    @PostMapping(value = "/{roomUuid}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "이미지 메시지 전송",
            description = """
                    채팅방에 이미지를 전송합니다. 전송 시 WebSocket 구독자(`/topic/chat/{roomUuid}`)에게도 실시간 브로드캐스트됩니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Request (multipart/form-data):**
                    - `image` (필수): 이미지 파일 (JPEG, PNG, GIF, WebP, 최대 10MB)

                    **Response:**
                    - `messageType`: `IMAGE`
                    - `content`: null
                    - `attachmentUrl`: 업로드된 이미지 경로

                    **WebSocket 실시간 수신:**
                    - 구독 경로: `/topic/chat/{roomUuid}`
                    - 수신 형식: `{ "eventType": "MESSAGE_CREATED", "payload": { ChatMessageResponse } }`
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "이미지 전송 성공"),
            @ApiResponse(responseCode = "400", description = "이미지 파일 없음 또는 지원하지 않는 형식",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "해당 채팅방의 멤버가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ChatMessageResponse> sendImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String roomUuid,
            @RequestPart("image") MultipartFile image) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.status(201).body(chatService.sendImageMessage(userId, roomUuid, image));
    }

    @PostMapping("/{roomUuid}/route")
    @Operation(
            summary = "루트 공유",
            description = """
                    채팅방에 루트를 공유합니다. 공유 시 WebSocket 구독자(`/topic/chat/{roomUuid}`)에게 실시간 브로드캐스트되며, 채팅방 멤버들에게 `CHAT_ROUTE_SHARED` 알림이 발송됩니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Request Body:**
                    - `routeUuid` (필수): 공유할 루트의 UUID

                    **Response:**
                    - `messageType`: `ROUTE`
                    - `content`: 루트 제목
                    - `routeUuid` / `routeTitle` / `routeThumbnailUrl`: 공유된 루트 정보
                    - `senderUuid` / `senderNickname` / `senderProfileImageUrl`: 발신자 정보
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "루트 공유 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "해당 채팅방의 멤버가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방 또는 루트",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ChatMessageResponse> shareRoute(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String roomUuid,
            @Valid @RequestBody ShareRouteRequest req) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.status(201).body(chatService.shareRoute(userId, roomUuid, req));
    }

    @PatchMapping(value = "/{roomUuid}/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "채팅방 프로필 이미지 변경",
            description = """
                    그룹 채팅방의 프로필 이미지를 변경합니다. ADMIN 권한(방장)만 가능합니다.
                    1:1 채팅방에는 사용할 수 없습니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Request (multipart/form-data):**
                    - `image` (필수): 업로드할 이미지 파일 (JPEG, PNG, GIF, WebP, 최대 10MB)

                    **Response:** 변경된 채팅방 정보 (`profileImageUrl` 업데이트됨)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "프로필 이미지 변경 성공"),
            @ApiResponse(responseCode = "400", description = "파일이 없거나 이미지 형식이 아님 / 1:1 채팅방",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 없음 또는 채팅방 멤버가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ChatRoomResponse> updateRoomProfileImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String roomUuid,
            @RequestPart("image") MultipartFile image) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(chatService.updateRoomProfileImage(userId, roomUuid, image));
    }

    @DeleteMapping("/{roomUuid}/profile-image")
    @Operation(
            summary = "채팅방 프로필 이미지 초기화",
            description = """
                    그룹 채팅방의 프로필 이미지를 5개 기본 이미지 중 하나로 랜덤 초기화합니다. ADMIN 권한(방장)만 가능합니다.
                    직접 업로드한 이미지가 있으면 삭제됩니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Response:** 변경된 채팅방 정보 (`profileImageUrl`이 기본 이미지 경로로 변경됨)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "프로필 이미지 초기화 성공"),
            @ApiResponse(responseCode = "400", description = "1:1 채팅방은 프로필 이미지 설정 불가",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN 권한 없음 또는 채팅방 멤버가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ChatRoomResponse> resetRoomProfileImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String roomUuid) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(chatService.resetRoomProfileImage(userId, roomUuid));
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
