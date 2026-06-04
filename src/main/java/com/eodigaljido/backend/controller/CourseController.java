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
@Tag(name = "Course", description = "코스(공유루트/내루트) API")
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

                    로그인 상태이면 각 아이템에 **`savedByMe`** 필드가 포함됩니다.
                    미로그인 시 `savedByMe`는 항상 `false`입니다.
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
            description = "코스 UUID로 상세 정보, 경유지(`routeSteps`), 리뷰(`reviews`)를 조회합니다. 조회할 때마다 `views`(조회수)가 1 증가합니다. 인증 없이 접근 가능합니다. 로그인 시 `savedByMe` 필드가 정확하게 반환됩니다.",
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
            @PathVariable String courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userDetails != null ? Long.parseLong(userDetails.getUsername()) : null;
        return ResponseEntity.ok(courseService.getCourseDetail(courseId, userId));
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
    // 저장한 코스 목록 (페이징)
    // ──────────────────────────────────────────────────────────

    @GetMapping("/saved")
    @Operation(
            summary = "저장한 코스 목록 조회",
            description = """
                    로그인 사용자가 저장(북마크)한 공유 코스 목록을 페이징하여 반환합니다.

                    - `userUuid` 생략 시: **로그인 본인**의 저장 목록
                    - `userUuid` 지정 시: **해당 사용자**의 저장 목록 (타인 프로필 조회)

                    응답 아이템의 `savedByMe`는 **로그인 본인** 기준으로 표시됩니다.
                    타인 목록을 조회할 때 내가 저장한 항목이면 `savedByMe: true`입니다.
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "저장한 코스 목록 반환",
                    content = @Content(schema = @Schema(implementation = CoursePageResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "userUuid에 해당하는 사용자 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CoursePageResponse> getSavedCourses(
            @Parameter(description = "조회할 사용자 UUID (생략 시 본인)", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam(required = false) String userUuid,
            @Parameter(description = "정렬 기준 (현재 latest 고정)") @RequestParam(required = false) String sort,
            @Parameter(description = "페이지 번호 (0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (최대 200)") @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        int clampedSize = Math.min(size, 200);
        return ResponseEntity.ok(courseService.getSavedCoursesPage(userId, userUuid, sort, page, clampedSize));
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
            summary = "개인 루트 생성",
            description = """
                    나만의 개인 루트를 생성합니다. 모임과 무관한 루트입니다.

                    - `title` (필수): 루트 이름 (최대 100자)
                    - `stops` (선택): 경유지 목록 — `kind`(start|via|end), `title`, `timeLine`, `lat`, `lng`
                    - `legs` (선택): 이동 구간 목록 — `mode`(walk|transit|car|bike), `minutes`, `transitType`, `directionsSummary`, `directionsDetail`, `distanceMeters`
                    - `tags` (선택, 최대 2개): 허용값 `산책 카페 맛집 데이트 관광 야경 쇼핑 역사 해변 가족 운동 반려동물`

                    모임 내 공동 루트 생성은 `POST /api/courses/group`을 사용하세요.
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
    // 모임 루트 생성
    // ──────────────────────────────────────────────────────────

    @PostMapping("/group")
    @Operation(
            summary = "모임 루트 생성",
            description = """
                    모임에 속한 공동 루트를 생성합니다. 모임 멤버만 사용할 수 있습니다.

                    - `title` (필수): 루트 이름 (최대 100자)
                    - `groupUuid` (필수): 소속 모임 UUID
                    - `stops` (선택): 경유지 목록 — `kind`(start|via|end), `title`, `timeLine`, `lat`, `lng`
                    - `legs` (선택): 이동 구간 목록 — `mode`(walk|transit|car|bike), `minutes`, `transitType`, `directionsSummary`, `directionsDetail`, `distanceMeters`
                    - `tags` (선택, 최대 2개): 허용값 `산책 카페 맛집 데이트 관광 야경 쇼핑 역사 해변 가족 운동 반려동물`
                    - `createChatRoom` (선택, 기본값 `true`): 루트 전용 채팅방 생성 여부. `false`로 설정하면 채팅방 없이 루트만 생성됩니다.

                    **생성 시 자동 처리:**
                    - `createChatRoom`이 `true`(기본)이면 루트 전용 채팅방이 생성되며 응답의 `chatRoomUuid`로 확인할 수 있습니다.
                    - `createChatRoom`이 `false`이면 채팅방이 생성되지 않으며 응답의 `chatRoomUuid`는 `null`입니다.
                    - 응답의 `collaborative`가 `true`로 반환됩니다.
                    - 모임 멤버 전원이 이 루트를 수정할 수 있습니다. 삭제는 소유자 또는 방장만 가능합니다.
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "모임 루트 생성 성공",
                    content = @Content(schema = @Schema(implementation = MyCourseDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 값 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "모임 멤버가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "모임을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MyCourseDetailResponse> createGroupCourse(
            @Valid @RequestBody CreateGroupCourseRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.status(201).body(courseService.createGroupCourse(userId, request));
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
                    루트의 상세 정보를 stops/legs 포맷으로 반환합니다.
                    - 개인 루트: 본인만 조회 가능합니다.
                    - 모임 루트(`collaborative: true`): 소유자 또는 해당 모임 멤버가 조회 가능합니다.
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "루트 상세 반환",
                    content = @Content(schema = @Schema(implementation = MyCourseDetailResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "조회 권한 없음 (본인 코스가 아니거나 모임 멤버가 아님)",
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
    // 내 코스 삭제
    // ──────────────────────────────────────────────────────────

    @DeleteMapping("/my/{courseId}")
    @Operation(
            summary = "내 코스 삭제",
            description = """
                    코스를 삭제합니다.
                    - **직접 만든 개인 루트**: 소프트 삭제 (status → DELETED)
                    - **모임 루트**: 소유자 또는 모임 방장(ADMIN)만 소프트 삭제 가능
                    - **저장한 코스**: 즐겨찾기 취소 (원본 코스는 삭제되지 않음)
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "삭제 권한 없음 (모임 루트는 소유자 또는 방장만 삭제 가능)",
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
                    루트의 정보를 수정합니다. 제공하지 않은 필드는 기존 값을 유지합니다.
                    - **개인 루트**: 소유자만 수정 가능합니다.
                    - **모임 루트**: 소유자 또는 해당 모임 멤버가 수정 가능합니다.

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
            @ApiResponse(responseCode = "403", description = "수정 권한 없음 (모임 루트는 소유자 또는 모임 멤버만 수정 가능)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MyCourseDetailResponse> updateMyCourse(
            @Parameter(description = "코스 UUID", required = true) @PathVariable String courseId,
            @Valid @RequestBody CreateMyCourseRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(courseService.updateMyCourseCollaborative(userId, courseId, request));
    }

    // ──────────────────────────────────────────────────────────
    // 내 루트 대표 이미지
    // ──────────────────────────────────────────────────────────

    @GetMapping("/collaborative/{courseId}")
    @Operation(summary = "공동 루트 진입 정보 조회", description = "공동 루트 멤버만 조회할 수 있습니다.", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<CollaborativeCourseResponse> getCollaborativeCourse(
            @PathVariable String courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(courseService.getCollaborativeCourse(userId, courseId));
    }

    @GetMapping("/my/{courseId}/members")
    @Operation(summary = "공동 루트 멤버 목록 조회", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<CourseMemberListResponse> getCourseMembers(
            @PathVariable String courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(courseService.getCourseMembers(userId, courseId));
    }

    @PostMapping("/my/{courseId}/members")
    @Operation(summary = "공동 루트 멤버 추가", description = "OWNER 또는 EDITOR만 멤버를 추가할 수 있습니다.", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<CollaborativeMemberResponse> addCourseMember(
            @PathVariable String courseId,
            @Valid @RequestBody AddCourseMemberRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.status(201).body(courseService.addCourseMember(userId, courseId, request));
    }

    @DeleteMapping("/my/{courseId}/members/{userUuid}")
    @Operation(summary = "공동 루트 멤버 제거", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<Void> removeCourseMember(
            @PathVariable String courseId,
            @PathVariable String userUuid,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        courseService.removeCourseMember(userId, courseId, userUuid);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/my/{courseId}/members/me")
    @Operation(summary = "공동 루트 나가기", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<Void> leaveCourse(
            @PathVariable String courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        courseService.leaveCourse(userId, courseId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/my/{courseId}/chat-room")
    @Operation(summary = "공동 루트 채팅방 연결", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<CourseChatRoomResponse> linkCourseChatRoom(
            @PathVariable String courseId,
            @Valid @RequestBody LinkCourseChatRoomRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(courseService.linkCourseChatRoom(userId, courseId, request));
    }

    @GetMapping("/my/{courseId}/chat-room")
    @Operation(summary = "공동 루트 채팅방 조회", security = @SecurityRequirement(name = "Bearer"))
    public ResponseEntity<CourseChatRoomResponse> getCourseChatRoom(
            @PathVariable String courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(courseService.getCourseChatRoom(userId, courseId));
    }

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
}
