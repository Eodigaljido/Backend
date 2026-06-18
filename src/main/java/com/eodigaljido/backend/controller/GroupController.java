package com.eodigaljido.backend.controller;

import com.eodigaljido.backend.dto.common.ErrorResponse;
import com.eodigaljido.backend.dto.course.CourseItemResponse;
import com.eodigaljido.backend.dto.group.*;
import com.eodigaljido.backend.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Tag(name = "Group", description = "모임 API")
public class GroupController {

    private final GroupService groupService;

    // ──────────────────────────────────────────────────────────
    // 모임 생성
    // ──────────────────────────────────────────────────────────

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "모임 생성",
            description = """
                    새 모임을 생성합니다. 생성자가 자동으로 방장(ADMIN)이 됩니다.

                    - `type`: `PUBLIC` (기본) 또는 `PRIVATE`
                    - `requiresApproval`: `PUBLIC` 모임에서 가입 시 승인이 필요한지 여부 (기본 `false`)
                    - `PRIVATE` 모임은 초대 링크를 통해서만 가입 가능합니다.
                    - `image`: 모임 프로필 이미지 파일 (선택, JPEG/PNG/GIF/WebP, 최대 10MB)
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    encoding = @Encoding(name = "request", contentType = MediaType.APPLICATION_JSON_VALUE)
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "모임 생성 성공",
                    content = @Content(schema = @Schema(implementation = GroupResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 값 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<GroupResponse> createGroup(
            @RequestPart("request") @Valid CreateGroupRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.status(201).body(groupService.createGroup(userId, request, image));
    }

    // ──────────────────────────────────────────────────────────
    // 공개 모임 목록
    // ──────────────────────────────────────────────────────────

    @GetMapping
    @Operation(
            summary = "공개 모임 목록 조회",
            description = "공개(PUBLIC) 모임 목록을 조회수 순으로 반환합니다. 인증 없이 접근 가능합니다.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "모임 목록 반환")
    })
    public ResponseEntity<Page<GroupResponse>> getPublicGroups(
            @Parameter(description = "페이지 번호 (0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userDetails != null ? Long.parseLong(userDetails.getUsername()) : null;
        return ResponseEntity.ok(groupService.getPublicGroups(userId, page, size));
    }

    // ──────────────────────────────────────────────────────────
    // 내가 속한 모임 목록
    // ──────────────────────────────────────────────────────────

    @GetMapping("/me")
    @Operation(
            summary = "내 모임 목록 조회",
            description = "내가 현재 가입되어 있는 모임 목록을 최근 가입순으로 반환합니다.",
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "모임 목록 반환"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Page<MyGroupResponse>> getMyGroups(
            @Parameter(description = "페이지 번호 (0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(groupService.getMyGroups(userId, page, size));
    }

    // ──────────────────────────────────────────────────────────
    // 모임 상세 조회
    // ──────────────────────────────────────────────────────────

    @GetMapping("/{groupUuid}")
    @Operation(
            summary = "모임 상세 조회",
            description = "모임 UUID로 상세 정보를 조회합니다. 조회할 때마다 `viewCount`가 1 증가합니다. 멤버 목록이 함께 반환됩니다.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "모임 상세 반환",
                    content = @Content(schema = @Schema(implementation = GroupDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "모임을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<GroupDetailResponse> getGroup(
            @Parameter(description = "모임 UUID", required = true) @PathVariable String groupUuid,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userDetails != null ? Long.parseLong(userDetails.getUsername()) : null;
        return ResponseEntity.ok(groupService.getGroup(groupUuid, userId));
    }

    // ──────────────────────────────────────────────────────────
    // 모임 수정
    // ──────────────────────────────────────────────────────────

    @PatchMapping("/{groupUuid}")
    @Operation(
            summary = "모임 수정",
            description = "모임 정보를 수정합니다. 방장(ADMIN)만 수행할 수 있습니다.",
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = GroupResponse.class))),
            @ApiResponse(responseCode = "403", description = "방장이 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "모임을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<GroupResponse> updateGroup(
            @Parameter(description = "모임 UUID", required = true) @PathVariable String groupUuid,
            @Valid @RequestBody CreateGroupRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(groupService.updateGroup(userId, groupUuid, request));
    }

    // ──────────────────────────────────────────────────────────
    // 모임 삭제
    // ──────────────────────────────────────────────────────────

    @DeleteMapping("/{groupUuid}")
    @Operation(
            summary = "모임 삭제",
            description = "모임을 삭제합니다 (소프트 삭제). 방장(ADMIN)만 수행할 수 있습니다.",
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "방장이 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "모임을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteGroup(
            @Parameter(description = "모임 UUID", required = true) @PathVariable String groupUuid,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        groupService.deleteGroup(userId, groupUuid);
        return ResponseEntity.noContent().build();
    }

    // ──────────────────────────────────────────────────────────
    // 모임 가입
    // ──────────────────────────────────────────────────────────

    @PostMapping("/{groupUuid}/join")
    @Operation(
            summary = "모임 가입",
            description = """
                    공개(PUBLIC) 모임에 가입합니다.
                    - `requiresApproval=false`: 즉시 가입됩니다. `status: "JOINED"`
                    - `requiresApproval=true`: 방장 승인 후 가입됩니다. `status: "PENDING"`

                    비공개(PRIVATE) 모임은 초대 링크를 통해서만 가입 가능합니다.
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "가입 완료(JOINED) 또는 대기(PENDING)",
                    content = @Content(schema = @Schema(implementation = GroupJoinResponse.class))),
            @ApiResponse(responseCode = "403", description = "비공개 모임",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 참여 중 또는 대기 요청 있음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<GroupJoinResponse> joinGroup(
            @Parameter(description = "모임 UUID", required = true) @PathVariable String groupUuid,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(groupService.joinGroup(userId, groupUuid));
    }

    // ──────────────────────────────────────────────────────────
    // 모임 탈퇴
    // ──────────────────────────────────────────────────────────

    @DeleteMapping("/{groupUuid}/members/me")
    @Operation(
            summary = "모임 탈퇴",
            description = "모임에서 탈퇴합니다. 방장은 탈퇴할 수 없습니다.",
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "탈퇴 성공"),
            @ApiResponse(responseCode = "400", description = "방장은 탈퇴 불가",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "모임 또는 멤버를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> leaveGroup(
            @Parameter(description = "모임 UUID", required = true) @PathVariable String groupUuid,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        groupService.leaveGroup(userId, groupUuid);
        return ResponseEntity.noContent().build();
    }

    // ──────────────────────────────────────────────────────────
    // 가입 요청 목록 (방장)
    // ──────────────────────────────────────────────────────────

    @GetMapping("/{groupUuid}/join-requests")
    @Operation(
            summary = "모임 가입 요청 목록 조회",
            description = "대기 중인 가입 요청 목록을 조회합니다. 방장만 조회할 수 있습니다.",
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "요청 목록 반환",
                    content = @Content(schema = @Schema(implementation = GroupJoinRequestResponse.class))),
            @ApiResponse(responseCode = "403", description = "방장이 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Page<GroupJoinRequestResponse>> getJoinRequests(
            @Parameter(description = "모임 UUID", required = true) @PathVariable String groupUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(groupService.getJoinRequests(userId, groupUuid, page, size));
    }

    // ──────────────────────────────────────────────────────────
    // 가입 요청 처리 (방장)
    // ──────────────────────────────────────────────────────────

    @PostMapping("/join-requests/{requestId}")
    @Operation(
            summary = "모임 가입 요청 승인/거절",
            description = "가입 요청을 승인하거나 거절합니다. 방장만 처리할 수 있습니다.",
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "처리 성공"),
            @ApiResponse(responseCode = "403", description = "방장이 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "요청을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 처리된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> processJoinRequest(
            @Parameter(description = "가입 요청 ID", required = true) @PathVariable Long requestId,
            @Valid @RequestBody ProcessGroupJoinRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        groupService.processJoinRequest(userId, requestId, request.action());
        return ResponseEntity.noContent().build();
    }

    // ──────────────────────────────────────────────────────────
    // 멤버 목록
    // ──────────────────────────────────────────────────────────

    @GetMapping("/{groupUuid}/members")
    @Operation(
            summary = "모임 멤버 목록 조회",
            description = "모임 멤버 목록을 조회합니다. 인증 없이 접근 가능합니다.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "멤버 목록 반환",
                    content = @Content(schema = @Schema(implementation = GroupMemberResponse.class))),
            @ApiResponse(responseCode = "404", description = "모임을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Page<GroupMemberResponse>> getMembers(
            @Parameter(description = "모임 UUID", required = true) @PathVariable String groupUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(groupService.getMembers(groupUuid, page, size));
    }

    // ──────────────────────────────────────────────────────────
    // 채팅방 생성 (방장)
    // ──────────────────────────────────────────────────────────

    @PostMapping("/{groupUuid}/chat-rooms")
    @Operation(
            summary = "모임 채팅방 생성",
            description = "모임 내 새 채팅방을 생성합니다. 방장만 생성할 수 있습니다.",
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "채팅방 생성 성공",
                    content = @Content(schema = @Schema(implementation = GroupChatRoomResponse.class))),
            @ApiResponse(responseCode = "403", description = "방장이 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<GroupChatRoomResponse> createChatRoom(
            @Parameter(description = "모임 UUID", required = true) @PathVariable String groupUuid,
            @Valid @RequestBody CreateGroupChatRoomRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.status(201).body(groupService.createChatRoom(userId, groupUuid, request));
    }

    // ──────────────────────────────────────────────────────────
    // 채팅방 목록
    // ──────────────────────────────────────────────────────────

    @GetMapping("/{groupUuid}/chat-rooms")
    @Operation(
            summary = "모임 채팅방 목록 조회",
            description = "모임에 속한 채팅방 목록을 조회합니다. 모임 멤버만 조회할 수 있습니다.",
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "채팅방 목록 반환",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = GroupChatRoomResponse.class)))),
            @ApiResponse(responseCode = "403", description = "모임 멤버가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<GroupChatRoomResponse>> getChatRooms(
            @Parameter(description = "모임 UUID", required = true) @PathVariable String groupUuid,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(groupService.getChatRooms(userId, groupUuid));
    }

    // ──────────────────────────────────────────────────────────
    // 게시물 작성
    // ──────────────────────────────────────────────────────────

    @PostMapping(value = "/{groupUuid}/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "모임 게시물 작성",
            description = """
                    모임에 게시물을 작성합니다. 모임 멤버만 작성할 수 있습니다.

                    **Request (multipart/form-data):**
                    - `content` (필수, 텍스트): 게시물 내용
                    - `images` (선택): 이미지 파일 목록 (JPEG·PNG·GIF·WebP, 최대 10MB, 최대 10장)
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "게시물 작성 성공",
                    content = @Content(schema = @Schema(implementation = GroupPostResponse.class))),
            @ApiResponse(responseCode = "400", description = "이미지 형식 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "모임 멤버가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<GroupPostResponse> createPost(
            @Parameter(description = "모임 UUID", required = true) @PathVariable String groupUuid,
            @RequestParam @NotBlank String content,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.status(201).body(groupService.createPost(userId, groupUuid, content, images));
    }

    // ──────────────────────────────────────────────────────────
    // 게시물 목록
    // ──────────────────────────────────────────────────────────

    @GetMapping("/{groupUuid}/posts")
    @Operation(
            summary = "모임 게시물 목록 조회",
            description = "모임 게시물 목록을 최신순으로 반환합니다. 모임 멤버만 조회할 수 있습니다.",
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시물 목록 반환",
                    content = @Content(schema = @Schema(implementation = GroupPostResponse.class))),
            @ApiResponse(responseCode = "403", description = "모임 멤버가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Page<GroupPostResponse>> getPosts(
            @Parameter(description = "모임 UUID", required = true) @PathVariable String groupUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(groupService.getPosts(userId, groupUuid, page, size));
    }

    // ──────────────────────────────────────────────────────────
    // 게시물 상세조회
    // ──────────────────────────────────────────────────────────

    @GetMapping("/{groupUuid}/posts/{postUuid}")
    @Operation(
            summary = "모임 게시물 상세조회",
            description = "게시물 UUID로 상세 정보를 조회합니다. 모임 멤버만 조회할 수 있습니다.",
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "게시물 상세 반환",
                    content = @Content(schema = @Schema(implementation = GroupPostResponse.class))),
            @ApiResponse(responseCode = "403", description = "모임 멤버가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "게시물을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<GroupPostResponse> getPost(
            @Parameter(description = "모임 UUID", required = true) @PathVariable String groupUuid,
            @Parameter(description = "게시물 UUID", required = true) @PathVariable String postUuid,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(groupService.getPost(userId, groupUuid, postUuid));
    }

    // ──────────────────────────────────────────────────────────
    // 게시물 수정
    // ──────────────────────────────────────────────────────────

    @PatchMapping(value = "/posts/{postUuid}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "모임 게시물 수정",
            description = """
                    게시물을 수정합니다. 작성자만 수정할 수 있습니다.

                    **Request (multipart/form-data):**
                    - `content` (필수, 텍스트): 게시물 내용
                    - `images` (선택): 새 이미지 파일 목록 (JPEG·PNG·GIF·WebP, 최대 10MB). 전달하면 기존 이미지를 교체하고, 생략하면 기존 이미지 유지
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = GroupPostResponse.class))),
            @ApiResponse(responseCode = "400", description = "이미지 형식 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "작성자가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "게시물을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<GroupPostResponse> updatePost(
            @Parameter(description = "게시물 UUID", required = true) @PathVariable String postUuid,
            @RequestParam @NotBlank String content,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(groupService.updatePost(userId, postUuid, content, images));
    }

    // ──────────────────────────────────────────────────────────
    // 게시물 삭제
    // ──────────────────────────────────────────────────────────

    @DeleteMapping("/{groupUuid}/posts/{postUuid}")
    @Operation(
            summary = "모임 게시물 삭제",
            description = "게시물을 삭제합니다. 작성자 또는 방장이 삭제할 수 있습니다.",
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "삭제 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "게시물을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deletePost(
            @Parameter(description = "모임 UUID", required = true) @PathVariable String groupUuid,
            @Parameter(description = "게시물 UUID", required = true) @PathVariable String postUuid,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        groupService.deletePost(userId, groupUuid, postUuid);
        return ResponseEntity.noContent().build();
    }

    // ──────────────────────────────────────────────────────────
    // 모임 내 루트 목록
    // ──────────────────────────────────────────────────────────

    @GetMapping("/{groupUuid}/routes")
    @Operation(
            summary = "모임 내 루트 목록 조회",
            description = "모임에 등록된 루트 목록을 반환합니다. 모임 멤버만 조회할 수 있습니다.",
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "루트 목록 반환",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CourseItemResponse.class)))),
            @ApiResponse(responseCode = "403", description = "모임 멤버가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<CourseItemResponse>> getGroupRoutes(
            @Parameter(description = "모임 UUID", required = true) @PathVariable String groupUuid,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(groupService.getGroupRoutes(userId, groupUuid));
    }
}
