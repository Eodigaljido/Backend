package com.eodigaljido.backend.controller;

import com.eodigaljido.backend.domain.route.Route.RouteStatus;
import com.eodigaljido.backend.dto.common.ErrorResponse;
import com.eodigaljido.backend.dto.course.*;
import com.eodigaljido.backend.service.CourseService;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Tag(name = "Course", description = "코스(공유루트/내루트/공동루트/서브루트) API")
public class CourseController {

    private final CourseService courseService;

    // ──────────────────────────────────────────────────────────
    // 공유 코스 목록
    // ──────────────────────────────────────────────────────────

    @GetMapping("/public")
    @Operation(
            summary = "공유 코스 목록 조회",
            description = """
                    공유된 코스 목록을 페이징하여 조회합니다. 인증 없이 접근 가능합니다.

                    `friends` 탭은 인증 토큰이 있는 경우에만 친구 필터가 적용되며,
                    미인증 시에는 전체 목록과 동일하게 반환됩니다.
                    """,
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "코스 목록 반환",
                    content = @Content(schema = @Schema(implementation = CoursePageResponse.class)))
    })
    public ResponseEntity<CoursePageResponse> getPublicCourses(
            @Parameter(description = "탭 구분: all | popular | date | friends", example = "all")
            @RequestParam(required = false) String tab,
            @Parameter(description = "활동 유형 필터 (예: 관광)", example = "관광")
            @RequestParam(required = false) String category,
            @Parameter(description = "지역 필터 (예: 서울)", example = "서울")
            @RequestParam(required = false) String region,
            @Parameter(description = "정렬 기준: popular | rating | date", example = "date")
            @RequestParam(required = false) String sort,
            @Parameter(description = "제목/설명 검색어", example = "고궁")
            @RequestParam(required = false) String q,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userDetails != null ? Long.parseLong(userDetails.getUsername()) : null;
        return ResponseEntity.ok(
                courseService.getPublicCourses(tab, category, region, sort, q, page, size, userId));
    }

    // ──────────────────────────────────────────────────────────
    // 공유 코스 preview
    // ──────────────────────────────────────────────────────────

    @GetMapping("/public/{courseId}/preview")
    @Operation(
            summary = "공유 코스 preview 조회",
            description = """
                    공유 링크·share-web OG 카드용 최소 필드를 반환합니다.
                    인증 없이 접근 가능합니다. 조회수는 증가하지 않습니다.

                    **Cache-Control:** `public, max-age=300` (5분 캐시)
                    """,
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "코스 preview 반환",
                    content = @Content(schema = @Schema(implementation = CoursePreviewResponse.class),
                            examples = @ExampleObject(name = "한강 데이트 코스 예시", value = """
                                    {
                                      "courseId": "7ecc5401-1234-5678-abcd-000000000001",
                                      "title": "한강 데이트 코스",
                                      "region": "서울",
                                      "category": "데이트",
                                      "durationLabel": "약 3시간",
                                      "thumbnailUrl": "https://cdn.example.com/thumb.jpg",
                                      "departure": "여의도역",
                                      "arrival": "뚝섬",
                                      "tags": ["야경", "산책"],
                                      "saveCount": 120,
                                      "rating": 4.50,
                                      "isPublic": true
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "없는 ID, 비공개, 삭제 코스",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CoursePreviewResponse> getCoursePreview(
            @Parameter(description = "코스 UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String courseId) {
        return ResponseEntity.ok()
                .header("Cache-Control", "public, max-age=300")
                .body(courseService.getCoursePreview(courseId));
    }

    // ──────────────────────────────────────────────────────────
    // 공유 코스 상세
    // ──────────────────────────────────────────────────────────

    @GetMapping("/{courseId}")
    @Operation(
            summary = "공유 코스 상세 조회",
            description = "코스 UUID로 상세 정보, 경유지(`routeSteps`), 리뷰(`reviews`)를 조회합니다. 조회할 때마다 `views`(조회수)가 1 증가합니다. 인증 없이 접근 가능합니다.",
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "코스 상세 반환",
                    content = @Content(schema = @Schema(implementation = CourseDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "없는 ID 또는 비공개 코스",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CourseDetailResponse> getCourseDetail(
            @Parameter(description = "코스 UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String courseId) {
        return ResponseEntity.ok(courseService.getCourseDetail(courseId));
    }

    // ──────────────────────────────────────────────────────────
    // 코스 즐겨찾기 저장
    // ──────────────────────────────────────────────────────────

    @PostMapping("/{courseId}/save")
    @Operation(
            summary = "코스 내 루트에 저장",
            description = "공유된 코스를 내 루트에 추가합니다. 같은 코스를 중복 저장할 수 없습니다.",
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "저장 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "없는 ID 또는 비공개 코스",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 저장된 코스",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> saveCourse(
            @Parameter(description = "코스 UUID", required = true) @PathVariable String courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        courseService.saveCourse(courseId, Long.parseLong(userDetails.getUsername()));
        return ResponseEntity.noContent().build();
    }

    // ──────────────────────────────────────────────────────────
    // 리뷰 작성
    // ──────────────────────────────────────────────────────────

    @PostMapping("/{courseId}/reviews")
    @Operation(
            summary = "코스 리뷰 작성",
            description = """
                    코스에 리뷰를 작성합니다.
                    - **인증 O**: 작성자 정보가 자동으로 설정됩니다. `userName` 무시.
                    - **인증 X**: `userName` 필드를 직접 제공해야 합니다. 미제공 시 '익명' 처리.
                    """,
            security = {}
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "리뷰 작성 성공",
                    content = @Content(schema = @Schema(implementation = ReviewResponse.class))),
            @ApiResponse(responseCode = "400", description = "rating 범위 오류 등 잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "없는 ID 또는 비공개 코스",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ReviewResponse> writeReview(
            @Parameter(description = "코스 UUID", required = true) @PathVariable String courseId,
            @Valid @RequestBody WriteReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userDetails != null ? Long.parseLong(userDetails.getUsername()) : null;
        return ResponseEntity.status(201).body(courseService.writeReview(courseId, request, userId));
    }

    // ──────────────────────────────────────────────────────────
    // 내 루트 생성
    // ──────────────────────────────────────────────────────────

    @PostMapping("/my")
    @Operation(
            summary = "내 루트 생성",
            description = """
                    새 루트를 생성합니다.

                    **Request Body:**
                    - `title` (필수): 루트 이름 (최대 100자)
                    - `collaborative` (선택, 기본 `false`): `true`로 설정하면 공동 루트로 생성되며, **루트 채팅방이 자동으로 생성**됩니다.
                    - `stops` (선택): 경유지 목록 — `kind`(start|via|end), `title`, `timeLine`, `lat`, `lng`
                    - `legs` (선택): 이동 구간 목록 — `mode`(walk|transit|car|bike), `minutes`, `transitType`, `directionsSummary`, `directionsDetail`, `distanceMeters`
                    - `tags` (선택, 최대 2개): 허용값 `산책 카페 맛집 데이트 관광 야경 쇼핑 역사 해변 가족 운동 반려동물`

                    **공동 루트(collaborative=true) 응답:**
                    - `chatRoomUuid` 필드에 자동 생성된 루트 채팅방 UUID가 포함됩니다.
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "루트 생성 성공",
                    content = @Content(schema = @Schema(implementation = MyCourseDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 값 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MyCourseDetailResponse> createMyCourse(
            @Valid @RequestBody CreateMyCourseRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.status(201).body(courseService.createMyCourse(userId, request));
    }

    // ──────────────────────────────────────────────────────────
    // 내 코스 목록
    // ──────────────────────────────────────────────────────────

    @GetMapping("/my")
    @Operation(
            summary = "내 코스 목록 조회",
            description = "내가 직접 만들었거나 저장한 코스 목록을 페이징하여 조회합니다. 검색·필터·정렬 지원.",
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "내 코스 목록 반환",
                    content = @Content(schema = @Schema(implementation = CoursePageResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CoursePageResponse> getMyCourses(
            @Parameter(description = "제목/설명 검색어") @RequestParam(required = false) String q,
            @Parameter(description = "활동 유형 필터") @RequestParam(required = false) String category,
            @Parameter(description = "지역 필터") @RequestParam(required = false) String region,
            @Parameter(description = "정렬 기준: popular | rating | date") @RequestParam(required = false) String sort,
            @Parameter(description = "페이지 번호 (0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(courseService.getMyCourses(userId, q, category, region, sort, page, size));
    }

    // ──────────────────────────────────────────────────────────
    // 내 루트 상세 조회
    // ──────────────────────────────────────────────────────────

    @GetMapping("/my/{courseId}")
    @Operation(
            summary = "내 루트 상세 조회",
            description = """
                    내 루트의 상세 정보를 조회합니다. stops/legs 포맷으로 반환합니다.

                    **공동 루트인 경우** `chatRoomUuid` 필드에 연결된 루트 채팅방 UUID가 포함됩니다.
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "루트 상세 반환",
                    content = @Content(schema = @Schema(implementation = MyCourseDetailResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인 코스가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MyCourseDetailResponse> getMyCourseDetail(
            @Parameter(description = "코스 UUID", required = true) @PathVariable String courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(courseService.getMyCourseDetail(userId, courseId));
    }

    // ──────────────────────────────────────────────────────────
    // 공동 루트 링크 진입
    // ──────────────────────────────────────────────────────────

    @GetMapping("/collaborative/{courseId}")
    @Operation(
            summary = "공동 루트 링크 진입 조회",
            description = """
                    `/routes/collaborative/{courseId}` 딥링크로 앱에 들어온 사용자가
                    해당 루트를 편집할 수 있는지 확인하고 루트 메타·경유지·이동 구간을 반환합니다.
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공동 루트 정보 반환",
                    content = @Content(schema = @Schema(implementation = CollaborativeCourseResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "공동 편집 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CollaborativeCourseResponse> getCollaborativeCourse(
            @Parameter(description = "코스 UUID", required = true) @PathVariable String courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(courseService.getCollaborativeCourse(userId, courseId));
    }

    // ──────────────────────────────────────────────────────────
    // 공동 루트 초대 (직접 입장 or 승인 필요)
    // ──────────────────────────────────────────────────────────

    @PostMapping("/my/{courseId}/invites")
    @Operation(
            summary = "공동 루트 초대",
            description = """
                    내 루트를 공동 편집 가능 상태로 전환하고 초대 정보를 반환합니다.
                    요청 본문에 `userId`를 포함하면 해당 친구를 초대합니다.

                    **초대 방식 (`requiresApproval`):**
                    - `false` (기본값): 초대 즉시 상대방을 채팅방 멤버로 추가합니다 **(직접 입장)**.
                    - `true`: 입장 요청 상태로 생성됩니다. 소유자가 `POST .../join-requests/{id}/approve`로 **승인해야** 입장됩니다.

                    **Request Body:** 생략 가능 (생략 시 링크만 활성화)
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "초대 성공 또는 입장 요청 생성",
                    content = @Content(schema = @Schema(implementation = CollaborativeInviteResponse.class))),
            @ApiResponse(responseCode = "400", description = "자기 자신 초대 등 잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인 코스가 아니거나 친구가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "코스 또는 초대 대상 사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 참여 중이거나 처리 대기 중인 요청 있음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CollaborativeInviteResponse> createCollaborativeInvite(
            @Parameter(description = "코스 UUID", required = true) @PathVariable String courseId,
            @RequestBody(required = false) CollaborativeInviteRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(courseService.createCollaborativeInvite(userId, courseId, request));
    }

    // ──────────────────────────────────────────────────────────
    // 입장 요청 목록 조회 (소유자 전용)
    // ──────────────────────────────────────────────────────────

    @GetMapping("/my/{courseId}/join-requests")
    @Operation(
            summary = "공동 루트 입장 요청 목록 조회",
            description = """
                    `requiresApproval=true`로 초대된 사용자들의 **PENDING(대기 중)** 입장 요청 목록을 반환합니다.
                    루트 소유자(방장)만 조회할 수 있습니다.

                    각 요청에는 `requestId`, 요청자 정보, 요청 시각이 포함됩니다.
                    `requestId`를 사용해 승인(`/approve`) 또는 거절(`/reject`)할 수 있습니다.
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "대기 중인 입장 요청 목록 반환",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = JoinRequestResponse.class)))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "소유자가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<JoinRequestResponse>> getJoinRequests(
            @Parameter(description = "코스 UUID", required = true) @PathVariable String courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(courseService.getJoinRequests(userId, courseId));
    }

    // ──────────────────────────────────────────────────────────
    // 입장 요청 승인/거절 (소유자 전용)
    // ──────────────────────────────────────────────────────────

    @PostMapping("/my/{courseId}/join-requests/{requestId}")
    @Operation(
            summary = "공동 루트 입장 요청 승인 또는 거절",
            description = """
                    대기 중인 입장 요청을 승인하거나 거절합니다.
                    루트 소유자(방장)만 처리할 수 있습니다.

                    **action 값:**
                    - `APPROVE`: 요청자를 채팅방 멤버로 추가하고 승인 알림을 발송합니다.
                    - `REJECT`: 요청을 거절하고 거절 알림을 발송합니다.
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "처리 성공 (승인 또는 거절)"),
            @ApiResponse(responseCode = "400", description = "해당 루트의 요청이 아니거나 action 값 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "소유자가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "코스 또는 입장 요청을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 처리된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> processJoinRequest(
            @Parameter(description = "코스 UUID", required = true) @PathVariable String courseId,
            @Parameter(description = "입장 요청 ID", required = true) @PathVariable Long requestId,
            @Valid @RequestBody ProcessJoinRequestRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        courseService.processJoinRequest(userId, courseId, requestId, request.action());
        return ResponseEntity.noContent().build();
    }

    // ──────────────────────────────────────────────────────────
    // 공동 루트 멤버 목록 조회
    // ──────────────────────────────────────────────────────────

    @GetMapping("/my/{courseId}/members")
    @Operation(
            summary = "공동 루트 멤버 목록 조회",
            description = """
                    공동 루트에 연결된 멤버 목록을 조회합니다.
                    소유자 또는 공동 루트 연결 채팅방 멤버만 조회할 수 있습니다.

                    아직 초대 링크가 활성화되지 않은 루트는 소유자 1명만 반환합니다.
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "멤버 목록 반환",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CollaborativeMemberResponse.class)))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "멤버 조회 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<CollaborativeMemberResponse>> getCollaborativeMembers(
            @Parameter(description = "코스 UUID", required = true) @PathVariable String courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(courseService.getCollaborativeMembers(userId, courseId));
    }

    // ──────────────────────────────────────────────────────────
    // 공동 루트 채팅방 UUID 조회
    // ──────────────────────────────────────────────────────────

    @GetMapping("/my/{courseId}/chat-room")
    @Operation(
            summary = "공동 루트 채팅방 UUID 조회",
            description = """
                    공동 루트에 연결된 채팅방 UUID를 반환합니다.
                    `collaborative=true`로 루트를 생성하면 채팅방이 자동 생성됩니다.
                    채팅방이 없는 경우 `chatRoomUuid`는 `null`입니다.
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "채팅방 UUID 반환 (없으면 null)",
                    content = @Content(schema = @Schema(implementation = CourseChatRoomResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인 코스가 아니거나 멤버 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CourseChatRoomResponse> getMyCourseChatRoom(
            @Parameter(description = "코스 UUID", required = true) @PathVariable String courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(courseService.getMyCourseChatRoom(userId, courseId));
    }

    // ──────────────────────────────────────────────────────────
    // 내 코스 삭제
    // ──────────────────────────────────────────────────────────

    @DeleteMapping("/my/{courseId}")
    @Operation(
            summary = "내 코스 삭제",
            description = """
                    내 코스를 삭제합니다.
                    - **직접 만든 코스**: 소프트 삭제 (status → DELETED)
                    - **저장한 코스**: 즐겨찾기 취소
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인 코스가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteMyCourse(
            @Parameter(description = "코스 UUID", required = true) @PathVariable String courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        courseService.deleteMyCourse(courseId, Long.parseLong(userDetails.getUsername()));
        return ResponseEntity.noContent().build();
    }

    // ──────────────────────────────────────────────────────────
    // 내 코스 수정
    // ──────────────────────────────────────────────────────────

    @PatchMapping("/my/{courseId}")
    @Operation(
            summary = "내 루트 수정",
            description = """
                    내 루트의 정보를 수정합니다. 제공하지 않은 필드는 기존 값을 유지합니다.

                    **태그 수정 정책:**
                    - `tags` 필드 **생략** → 기존 태그 유지
                    - `tags: []` (빈 배열) → 태그 전체 삭제
                    - `tags: ["산책", "카페"]` → 해당 값으로 교체 (최대 2개)

                    **허용 태그:** `산책 카페 맛집 데이트 관광 야경 쇼핑 역사 해변 가족 운동 반려동물`
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정된 루트 상세 반환",
                    content = @Content(schema = @Schema(implementation = MyCourseDetailResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인 코스가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MyCourseDetailResponse> updateMyCourse(
            @Parameter(description = "코스 UUID", required = true) @PathVariable String courseId,
            @Valid @RequestBody CreateMyCourseRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(courseService.updateMyCourse(userId, courseId, request));
    }

    // ──────────────────────────────────────────────────────────
    // 내 루트 대표 이미지
    // ──────────────────────────────────────────────────────────

    @PatchMapping(value = "/my/{courseId}/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "내 루트 대표 이미지 업로드",
            description = """
                    내 루트의 대표 이미지(썸네일)를 업로드합니다.

                    - multipart part 이름은 `image`를 우선 사용합니다.
                    - 기존 앱 폴백 호환을 위해 `file` part도 허용합니다.
                    - 허용 형식: JPEG, PNG, WebP
                    - 최대 용량: 10MB
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "대표 이미지 업로드 성공",
                    content = @Content(schema = @Schema(implementation = CourseThumbnailResponse.class))),
            @ApiResponse(responseCode = "400", description = "파일 누락/용량 초과/지원하지 않는 이미지 형식",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인 코스가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CourseThumbnailResponse> updateMyCourseThumbnail(
            @Parameter(description = "코스 UUID", required = true) @PathVariable String courseId,
            @Parameter(description = "대표 이미지 파일", required = true)
            @RequestPart(value = "image", required = false) MultipartFile image,
            @Parameter(description = "대표 이미지 파일(image part 폴백)", hidden = true)
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        MultipartFile upload = image != null ? image : file;
        return ResponseEntity.ok(courseService.updateMyCourseThumbnail(userId, courseId, upload));
    }

    @DeleteMapping("/my/{courseId}/thumbnail")
    @Operation(
            summary = "내 루트 대표 이미지 삭제",
            description = "내 루트의 대표 이미지를 삭제하고 이후 조회 응답에서 `thumbnail: null`로 반환합니다.",
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "대표 이미지 삭제 성공",
                    content = @Content(schema = @Schema(implementation = CourseThumbnailResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인 코스가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CourseThumbnailResponse> deleteMyCourseThumbnail(
            @Parameter(description = "코스 UUID", required = true) @PathVariable String courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(courseService.deleteMyCourseThumbnail(userId, courseId));
    }

    // ──────────────────────────────────────────────────────────
    // 내 루트 상태 변경
    // ──────────────────────────────────────────────────────────

    @PatchMapping("/my/{courseId}/status")
    @Operation(
            summary = "내 루트 상태 변경",
            description = "내 루트의 상태를 DRAFT 또는 PUBLISHED로 변경합니다. DELETED로는 변경 불가.",
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경된 루트 상세 반환",
                    content = @Content(schema = @Schema(implementation = MyCourseDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "DELETED로 변경 시도",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인 코스가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MyCourseDetailResponse> updateCourseStatus(
            @Parameter(description = "코스 UUID", required = true) @PathVariable String courseId,
            @Valid @RequestBody UpdateCourseStatusRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(courseService.updateCourseStatus(userId, courseId, request.status()));
    }

    // ──────────────────────────────────────────────────────────
    // 내가 공유 중인 코스 목록
    // ──────────────────────────────────────────────────────────

    @GetMapping("/my/sharing")
    @Operation(
            summary = "내가 공유 중인 코스 목록",
            description = "현재 공개(shared=true) 상태인 내 코스 목록을 조회합니다.",
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공유 중인 코스 목록 반환",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CourseItemResponse.class)))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<CourseItemResponse>> getSharingCourses(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(courseService.getSharingCourses(Long.parseLong(userDetails.getUsername())));
    }

    // ──────────────────────────────────────────────────────────
    // 내 루트 공유 활성화
    // ──────────────────────────────────────────────────────────

    @PostMapping("/my/{courseId}/share")
    @Operation(
            summary = "내 루트 공유 활성화",
            description = "내 루트를 공개합니다. 팔로워에게 팔로잉 소식이 발행되고, 취향이 맞는 사용자에게 추천 알림이 전송됩니다.",
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "공유 활성화 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인 코스가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> enableSharing(
            @Parameter(description = "코스 UUID", required = true) @PathVariable String courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        courseService.enableSharing(Long.parseLong(userDetails.getUsername()), courseId);
        return ResponseEntity.noContent().build();
    }

    // ──────────────────────────────────────────────────────────
    // 내 루트 공유 비활성화
    // ──────────────────────────────────────────────────────────

    @DeleteMapping("/my/{courseId}/share")
    @Operation(
            summary = "내 루트 공유 비활성화",
            description = "내 루트를 비공개로 전환합니다.",
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "공유 비활성화 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인 코스가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> disableSharing(
            @Parameter(description = "코스 UUID", required = true) @PathVariable String courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        courseService.disableSharing(Long.parseLong(userDetails.getUsername()), courseId);
        return ResponseEntity.noContent().build();
    }

    // ──────────────────────────────────────────────────────────
    // 저장 취소
    // ──────────────────────────────────────────────────────────

    @DeleteMapping("/{courseId}/save")
    @Operation(
            summary = "저장된 코스 취소",
            description = "내 루트에 저장했던 공유 코스를 제거합니다.",
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "저장 취소 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "코스 또는 저장 기록을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> unsaveCourse(
            @Parameter(description = "코스 UUID", required = true) @PathVariable String courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        courseService.unsaveCourse(Long.parseLong(userDetails.getUsername()), courseId);
        return ResponseEntity.noContent().build();
    }

    // ──────────────────────────────────────────────────────────
    // 코스 복사
    // ──────────────────────────────────────────────────────────

    @PostMapping("/{courseId}/copy")
    @Operation(
            summary = "코스 복사",
            description = "공유된 코스(또는 본인 코스)를 복사하여 내 루트로 추가합니다. 원본 소유자에게 알림이 전송됩니다.",
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "복사된 루트 상세 반환",
                    content = @Content(schema = @Schema(implementation = MyCourseDetailResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "공유되지 않은 코스",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MyCourseDetailResponse> copyCourse(
            @Parameter(description = "코스 UUID", required = true) @PathVariable String courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.status(201).body(courseService.copyCourse(userId, courseId));
    }

    // ──────────────────────────────────────────────────────────
    // 루트 채팅방 내 서브 루트 생성
    // ──────────────────────────────────────────────────────────

    @PostMapping("/rooms/{chatRoomUuid}/sub-routes")
    @Operation(
            summary = "서브 루트 생성",
            description = """
                    루트 채팅방(버섯) 내에서 새 서브 루트(마산버섯 등)를 생성합니다.

                    **동작:**
                    - 요청자는 해당 루트 채팅방의 멤버여야 합니다.
                    - 서브 루트용 자식 루트 채팅방이 자동으로 생성됩니다. 생성자만 초기 멤버로 추가됩니다.
                    - 나머지 참여자는 초대(`POST /api/courses/my/{courseId}/invites`)로 직접 합류해야 합니다.
                    - 생성된 서브 루트는 `collaborative=true` 상태이며 `chatRoomUuid`가 포함됩니다.

                    **Request Body:** `CreateMyCourseRequest`와 동일 (title 필수, stops/legs/tags 선택)
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "서브 루트 생성 성공",
                    content = @Content(schema = @Schema(implementation = SubCourseResponse.class))),
            @ApiResponse(responseCode = "400", description = "루트 채팅방이 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "채팅방 멤버가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SubCourseResponse> createSubCourse(
            @Parameter(description = "부모 루트 채팅방 UUID", required = true) @PathVariable String chatRoomUuid,
            @Valid @RequestBody CreateMyCourseRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.status(201).body(courseService.createSubCourse(userId, chatRoomUuid, request));
    }

    // ──────────────────────────────────────────────────────────
    // 루트 채팅방 내 서브 루트 목록 조회
    // ──────────────────────────────────────────────────────────

    @GetMapping("/rooms/{chatRoomUuid}/sub-routes")
    @Operation(
            summary = "서브 루트 목록 조회",
            description = """
                    루트 채팅방(버섯)에 귀속된 서브 루트(마산버섯, 창원버섯 등) 목록을 조회합니다.

                    각 항목에는 자식 채팅방 UUID, 연결된 루트 UUID, 마지막 메시지, 미읽음 수가 포함됩니다.
                    요청자는 해당 루트 채팅방의 멤버여야 합니다.
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "서브 루트 목록 반환",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SubCourseResponse.class)))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "채팅방 멤버가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<SubCourseResponse>> getSubCourses(
            @Parameter(description = "부모 루트 채팅방 UUID", required = true) @PathVariable String chatRoomUuid,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(courseService.getSubCourses(userId, chatRoomUuid));
    }
}
