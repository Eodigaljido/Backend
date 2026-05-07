package com.eodigaljido.backend.controller;

import com.eodigaljido.backend.dto.common.ErrorResponse;
import com.eodigaljido.backend.dto.course.*;
import com.eodigaljido.backend.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
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

import java.util.Map;

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
                    공유된 코스 목록을 조회합니다. 인증 없이 접근 가능합니다.

                    **Query Parameters:**
                    - `tab` (선택): `all` | `popular` | `date` | `friends`
                      `friends` 탭은 인증 토큰이 있는 경우에만 친구 필터가 적용됩니다.
                    - `category` (선택): 활동 유형 필터 (예: 관광)
                    - `region` (선택): 지역 필터 (예: 서울)
                    - `sort` (선택): `popular` | `rating` | `date`
                    - `q` (선택): 제목/설명 검색어
                    - `page` (선택, 기본 0): 페이지 번호
                    - `size` (선택, 기본 20): 페이지 크기
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "코스 목록 반환")
    })
    public ResponseEntity<CoursePageResponse> getPublicCourses(
            @RequestParam(required = false) String tab,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
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
                    코스 UUID로 상세 정보, 경유지(routeSteps), 리뷰(reviews)를 조회합니다.
                    조회 시 조회수(views)가 1 증가합니다. 인증 없이 접근 가능합니다.

                    **Path Variable:**
                    - `courseId`: 코스 UUID
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "코스 상세 정보 반환"),
            @ApiResponse(responseCode = "404", description = "존재하지 않거나 공유되지 않은 코스",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CourseDetailResponse> getCourseDetail(@PathVariable String courseId) {
        return ResponseEntity.ok(courseService.getCourseDetail(courseId));
    }

    // ──────────────────────────────────────────────────────────
    // 코스 내 루트에 저장 (인증 필요)
    // ──────────────────────────────────────────────────────────

    @PostMapping("/{courseId}/save")
    @Operation(
            summary = "코스 내 루트에 저장",
            description = """
                    공유된 코스를 내 루트에 추가합니다. 같은 코스를 중복 저장할 수 없습니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Path Variable:**
                    - `courseId`: 저장할 코스 UUID
                    """,
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

                    **헤더:** `Authorization: Bearer {accessToken}` (선택)
                    - 인증 시: 작성자 정보 자동 설정
                    - 미인증 시: `userName` 필드를 직접 제공해야 합니다.

                    **Request Body:**
                    - `userName` (선택): 비로그인 작성자 이름
                    - `rating` (필수): 평점 1~5
                    - `text` (필수): 리뷰 내용
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "리뷰 작성 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않거나 공유되지 않은 코스",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ReviewResponse> writeReview(
            @PathVariable String courseId,
            @Valid @RequestBody WriteReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userDetails != null ? Long.parseLong(userDetails.getUsername()) : null;
        ReviewResponse response = courseService.writeReview(courseId, request, userId);
        return ResponseEntity.status(201).body(response);
    }

    // ──────────────────────────────────────────────────────────
    // 내 코스 목록 (인증 필요)
    // ──────────────────────────────────────────────────────────

    @GetMapping("/my")
    @Operation(
            summary = "내 코스 목록 조회",
            description = """
                    내가 직접 만들었거나 저장한 코스 목록을 조회합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Query Parameters:**
                    - `q` (선택): 검색어
                    - `category` (선택): 카테고리 필터
                    - `region` (선택): 지역 필터
                    - `sort` (선택): `popular` | `rating` | `date`
                    - `page` (선택, 기본 0): 페이지 번호
                    - `size` (선택, 기본 20): 페이지 크기
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "내 코스 목록 반환"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CoursePageResponse> getMyCourses(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(
                courseService.getMyCourses(userId, q, category, region, sort, page, size));
    }

    // ──────────────────────────────────────────────────────────
    // 내 코스 삭제 (인증 필요)
    // ──────────────────────────────────────────────────────────

    @DeleteMapping("/my/{courseId}")
    @Operation(
            summary = "내 코스 삭제",
            description = """
                    내 코스를 삭제합니다.
                    - 직접 만든 코스: 소프트 삭제 (status=DELETED)
                    - 저장한 코스: 즐겨찾기 취소

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteMyCourse(
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
            summary = "내 코스 메타 수정",
            description = """
                    내 코스의 기본 정보(제목, 설명, 카테고리, 지역)를 수정합니다.
                    제공되지 않은 필드는 기존 값을 유지합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Request Body (모두 선택):**
                    - `title`: 코스 이름
                    - `description`: 코스 설명
                    - `category`: 카테고리 (활동 유형)
                    - `region`: 지역
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정된 코스 상세 정보 반환"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CourseDetailResponse> updateMyCourse(
            @PathVariable String courseId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(courseService.updateMyCourse(
                courseId, userId,
                body.get("title"),
                body.get("description"),
                body.get("category"),
                body.get("region")
        ));
    }
}
