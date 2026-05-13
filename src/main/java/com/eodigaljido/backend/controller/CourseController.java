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
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Tag(name = "Course", description = "코스(공유루트/내루트) API")
public class CourseController {

    private final CourseService courseService;

    // ──────────────────────────────────────────────────────────
    // 공유 코스 목록 (인증 불필요, friends 탭은 선택 인증)
    // ──────────────────────────────────────────────────────────

    @GetMapping("/public")
    @Operation(
            summary = "공유 코스 목록 조회",
            description = """
                    공유된 코스 목록을 페이징하여 조회합니다. 인증 없이 접근 가능합니다.

                    `friends` 탭은 인증 토큰이 있는 경우에만 친구 필터가 적용되며,
                    미인증 시에는 전체 목록과 동일하게 반환됩니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "코스 목록 반환",
                    content = @Content(schema = @Schema(implementation = CoursePageResponse.class))
            )
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
    // 공유 코스 상세 (인증 불필요)
    // ──────────────────────────────────────────────────────────

    @GetMapping("/{courseId}")
    @Operation(
            summary = "공유 코스 상세 조회",
            description = """
                    코스 UUID로 상세 정보, 경유지(`routeSteps`), 리뷰(`reviews`)를 조회합니다.
                    조회할 때마다 `views`(조회수)가 1 증가합니다. 인증 없이 접근 가능합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "코스 상세 정보 반환",
                    content = @Content(schema = @Schema(implementation = CourseDetailResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않거나 공유되지 않은 코스",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<CourseDetailResponse> getCourseDetail(
            @Parameter(description = "코스 UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String courseId) {
        return ResponseEntity.ok(courseService.getCourseDetail(courseId));
    }

    // ──────────────────────────────────────────────────────────
    // 코스 내 루트에 저장 (인증 필요)
    // ──────────────────────────────────────────────────────────

    @PostMapping("/{courseId}/save")
    @Operation(
            summary = "코스 내 루트에 저장",
            description = "공유된 코스를 내 루트에 추가합니다. 같은 코스를 중복 저장할 수 없습니다.",
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "저장 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않거나 공유되지 않은 코스",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 저장된 코스",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> saveCourse(
            @Parameter(description = "코스 UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        courseService.saveCourse(courseId, userId);
        return ResponseEntity.noContent().build();
    }

    // ──────────────────────────────────────────────────────────
    // 리뷰 작성 (인증 선택 — 비로그인도 userName으로 작성 가능)
    // ──────────────────────────────────────────────────────────

    @PostMapping("/{courseId}/reviews")
    @Operation(
            summary = "코스 리뷰 작성",
            description = """
                    코스에 리뷰를 작성합니다.

                    - **인증 O**: 작성자 정보가 자동으로 설정됩니다. `userName` 무시.
                    - **인증 X**: `userName` 필드를 직접 제공해야 합니다. 미제공 시 '익명' 처리.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "리뷰 작성 성공",
                    content = @Content(schema = @Schema(implementation = ReviewResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않음 (rating 범위 오류 등)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않거나 공유되지 않은 코스",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ReviewResponse> writeReview(
            @Parameter(description = "코스 UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String courseId,
            @Valid @RequestBody WriteReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userDetails != null ? Long.parseLong(userDetails.getUsername()) : null;
        ReviewResponse response = courseService.writeReview(courseId, request, userId);
        return ResponseEntity.status(201).body(response);
    }

    // ──────────────────────────────────────────────────────────
    // 내 루트 생성 (인증 필요)
    // ──────────────────────────────────────────────────────────

    @PostMapping("/my")
    @Operation(
            summary = "내 루트 생성",
            description = """
                    새 루트를 생성합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Request Body:**
                    - `title` (필수): 루트 이름 (최대 100자)
                    - `collaborative` (선택): 공유 여부, 기본 false
                    - `stops` (선택): 경유지 목록 — `kind`(start|via|end), `title`, `timeLine`, `lat`, `lng`
                    - `legs` (선택): 이동 구간 목록 — `mode`(walk|transit|car|bike), `minutes`, `transitType`, `directionsSummary`, `directionsDetail`, `distanceMeters`

                    **Response:** 생성된 루트 정보 (`uuid` 포함) — 201 Created
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "루트 생성 성공",
                    content = @Content(schema = @Schema(implementation = MyCourseDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MyCourseDetailResponse> createMyCourse(
            @Valid @RequestBody CreateMyCourseRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        MyCourseDetailResponse response = courseService.createMyCourse(userId, request);
        return ResponseEntity.status(201).body(response);
    }

    // ──────────────────────────────────────────────────────────
    // 내 코스 목록 (인증 필요)
    // ──────────────────────────────────────────────────────────

    @GetMapping("/my")
    @Operation(
            summary = "내 코스 목록 조회",
            description = "내가 직접 만들었거나 저장한 코스 목록을 페이징하여 조회합니다.",
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "내 코스 목록 반환",
                    content = @Content(schema = @Schema(implementation = CoursePageResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CoursePageResponse> getMyCourses(
            @Parameter(description = "제목/설명 검색어", example = "고궁")
            @RequestParam(required = false) String q,

            @Parameter(description = "활동 유형 필터 (예: 관광)", example = "관광")
            @RequestParam(required = false) String category,

            @Parameter(description = "지역 필터 (예: 서울)", example = "서울")
            @RequestParam(required = false) String region,

            @Parameter(description = "정렬 기준: popular | rating | date", example = "date")
            @RequestParam(required = false) String sort,

            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(
                courseService.getMyCourses(userId, q, category, region, sort, page, size));
    }

    // ──────────────────────────────────────────────────────────
    // 내 루트 상세 조회 (인증 필요)
    // ──────────────────────────────────────────────────────────

    @GetMapping("/my/{courseId}")
    @Operation(
            summary = "내 루트 상세 조회",
            description = """
                    내 루트의 상세 정보를 조회합니다. stops/legs 포맷으로 반환합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Response:**
                    - `uuid`: 루트 UUID
                    - `title`: 루트 이름
                    - `collaborative`: 공유 여부
                    - `stops`: 경유지 목록 (kind, title, timeLine, lat, lng)
                    - `legs`: 이동 구간 목록 (mode, minutes, transitType 등)
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "내 루트 상세 반환",
                    content = @Content(schema = @Schema(implementation = MyCourseDetailResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인 코스가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MyCourseDetailResponse> getMyCourseDetail(
            @Parameter(description = "코스 UUID", required = true)
            @PathVariable String courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(courseService.getMyCourseDetail(userId, courseId));
    }

    // ──────────────────────────────────────────────────────────
    // 내 코스 삭제 (인증 필요)
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
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인 코스가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteMyCourse(
            @Parameter(description = "코스 UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        courseService.deleteMyCourse(courseId, userId);
        return ResponseEntity.noContent().build();
    }

    // ──────────────────────────────────────────────────────────
    // 내 코스 수정 (인증 필요)
    // ──────────────────────────────────────────────────────────

    @PatchMapping("/my/{courseId}")
    @Operation(
            summary = "내 루트 수정",
            description = """
                    내 루트의 전체 정보(제목, 공유여부, stops, legs)를 수정합니다. 제공하지 않은 필드는 기존 값을 유지합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Response:** 수정된 루트 상세 정보 (stops/legs 포맷)
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정된 루트 상세 반환",
                    content = @Content(schema = @Schema(implementation = MyCourseDetailResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인 코스가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MyCourseDetailResponse> updateMyCourse(
            @Parameter(description = "코스 UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String courseId,
            @Valid @RequestBody CreateMyCourseRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(courseService.updateMyCourse(userId, courseId, request));
    }

    // ──────────────────────────────────────────────────────────
    // 내 루트 상태 변경 (인증 필요)
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
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인 코스가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MyCourseDetailResponse> updateCourseStatus(
            @Parameter(description = "코스 UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String courseId,
            @Valid @RequestBody UpdateCourseStatusRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(courseService.updateCourseStatus(userId, courseId, request.status()));
    }

    // ──────────────────────────────────────────────────────────
    // 내가 공유 중인 코스 목록 (인증 필요)
    // ──────────────────────────────────────────────────────────

    @GetMapping("/my/sharing")
    @Operation(
            summary = "내가 공유 중인 코스 목록",
            description = "현재 공유(공개) 상태인 내 코스 목록을 조회합니다.",
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공유 중인 코스 목록 반환",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CourseItemResponse.class)))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<CourseItemResponse>> getSharingCourses(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(courseService.getSharingCourses(userId));
    }

    // ──────────────────────────────────────────────────────────
    // 내 루트 공유 활성화 (인증 필요)
    // ──────────────────────────────────────────────────────────

    @PostMapping("/my/{courseId}/share")
    @Operation(
            summary = "내 루트 공유 활성화",
            description = "내 루트를 공개합니다. 팔로워에게 팔로잉 소식이 발행되고, 취향이 맞는 사용자에게 추천 알림이 전송됩니다.",
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "공유 활성화 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인 코스가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> enableSharing(
            @Parameter(description = "코스 UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        courseService.enableSharing(userId, courseId);
        return ResponseEntity.noContent().build();
    }

    // ──────────────────────────────────────────────────────────
    // 내 루트 공유 비활성화 (인증 필요)
    // ──────────────────────────────────────────────────────────

    @DeleteMapping("/my/{courseId}/share")
    @Operation(
            summary = "내 루트 공유 비활성화",
            description = "내 루트를 비공개로 전환합니다.",
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "공유 비활성화 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인 코스가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> disableSharing(
            @Parameter(description = "코스 UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        courseService.disableSharing(userId, courseId);
        return ResponseEntity.noContent().build();
    }

    // ──────────────────────────────────────────────────────────
    // 저장된 코스 취소 (인증 필요)
    // ──────────────────────────────────────────────────────────

    @DeleteMapping("/{courseId}/save")
    @Operation(
            summary = "저장된 코스 취소",
            description = "내 루트에 저장했던 공유 코스를 제거합니다.",
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "저장 취소 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "코스 또는 저장 기록을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> unsaveCourse(
            @Parameter(description = "코스 UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        courseService.unsaveCourse(userId, courseId);
        return ResponseEntity.noContent().build();
    }

    // ──────────────────────────────────────────────────────────
    // 코스 복사 (인증 필요)
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
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "공유되지 않은 코스",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MyCourseDetailResponse> copyCourse(
            @Parameter(description = "코스 UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.status(201).body(courseService.copyCourse(userId, courseId));
    }
}
