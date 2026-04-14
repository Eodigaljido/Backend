package com.eodigaljido.backend.controller;

import com.eodigaljido.backend.dto.route.*;
import com.eodigaljido.backend.dto.common.ErrorResponse;
import com.eodigaljido.backend.service.RouteService;
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

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/routes")
@RequiredArgsConstructor
@Tag(name = "Route", description = "루트(경로) API")
public class RouteController {

    private final RouteService routeService;

    // ──────────────────────────────────────────────────────────
    // CRUD
    // ──────────────────────────────────────────────────────────

    @PostMapping
    @Operation(
            summary = "루트 생성",
            description = """
                    새 루트와 경유지를 생성합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Request Body:**
                    - `title` (필수): 루트 이름, 최대 100자
                    - `description` (선택): 루트 설명
                    - `status` (선택): 초기 상태, `DRAFT` 또는 `PUBLISHED`. 생략 시 `DRAFT`로 저장
                    - `totalDistance` (선택): 총 거리 (km)
                    - `estimatedTime` (선택): 예상 소요시간 (분)
                    - `thumbnailUrl` (선택): 대표 이미지 URL
                    - `waypoints` (선택): 경유지 목록
                      - `sequence` (필수): 순서 (1부터 시작)
                      - `latitude` (필수): 위도
                      - `longitude` (필수): 경도
                      - `name`, `address`, `memo` (선택)

                    **Response:** 생성된 루트 상세 정보 + 경유지 목록 (201 Created)
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "루트 생성 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RouteResponse> createRoute(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateRouteRequest request) {
        Long userId = Long.parseLong(userDetails.getUsername());
        RouteResponse response = routeService.createRoute(userId, request);
        return ResponseEntity.created(URI.create("/routes/" + response.uuid())).body(response);
    }

    @GetMapping("/me")
    @Operation(
            summary = "내 루트 목록 조회",
            description = """
                    내가 만든 루트 목록을 반환합니다. 삭제된 루트(status=DELETED)는 포함되지 않습니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Response:** 루트 요약 목록 (경유지 미포함)
                    - `uuid`, `title`, `status`, `isShared`
                    - `totalDistance`, `estimatedTime`, `thumbnailUrl`, `createdAt`
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "내 루트 목록 반환"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<RouteSummaryResponse>> getMyRoutes(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(routeService.getMyRoutes(userId));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "내 루트 상세 조회",
            description = """
                    루트 ID로 상세 정보와 경유지 목록을 조회합니다. 본인 루트만 조회 가능합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Path Variable:**
                    - `id`: 조회할 루트의 ID (auto_increment)

                    **Response:** 루트 상세 정보 + 경유지 목록 (sequence 오름차순)
                    - `uuid`, `title`, `description`, `status`, `isShared`
                    - `totalDistance`, `estimatedTime`, `thumbnailUrl`
                    - `waypoints[]`, `createdAt`, `updatedAt`

                    타인의 루트 조회 시 403을 반환합니다.
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "루트 상세 정보 반환"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인 루트가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 루트",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RouteResponse> getRoute(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(routeService.getRoute(userId, id));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "루트 수정",
            description = """
                    루트 정보를 수정합니다. 경유지는 전체 삭제 후 새 목록으로 교체됩니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Path Variable:**
                    - `id`: 수정할 루트의 ID

                    **Request Body:**
                    - `title` (필수): 루트 이름
                    - `description` (선택): 루트 설명
                    - `totalDistance` (선택): 총 거리 (km)
                    - `estimatedTime` (선택): 예상 소요시간 (분)
                    - `thumbnailUrl` (선택): 대표 이미지 URL
                    - `waypoints` (선택): 경유지 목록 — 기존 경유지 전체를 새 목록으로 교체. 빈 배열 전달 시 모든 경유지 삭제

                    본인 루트가 아니면 403을 반환합니다.
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정된 루트 상세 정보 반환"),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인 루트가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 루트",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RouteResponse> updateRoute(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UpdateRouteRequest request) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(routeService.updateRoute(userId, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "루트 삭제",
            description = """
                    루트를 소프트 삭제합니다. status가 DELETED로 변경되며 목록 조회에서 제외됩니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Path Variable:**
                    - `id`: 삭제할 루트의 ID

                    본인 루트가 아니면 403을 반환합니다.
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "루트 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인 루트가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 루트",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteRoute(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Long userId = Long.parseLong(userDetails.getUsername());
        routeService.deleteRoute(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @Operation(
            summary = "루트 상태 변경",
            description = """
                    루트 상태를 DRAFT 또는 PUBLISHED로 변경합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Path Variable:**
                    - `id`: 상태를 변경할 루트의 ID

                    **Request Body:**
                    - `status` (필수): 변경할 상태
                      - `DRAFT` — 임시저장 상태
                      - `PUBLISHED` — 공개 상태
                      - `DELETED` 로는 변경 불가 (삭제는 `DELETE /routes/{id}` 사용)

                    본인 루트가 아니면 403을 반환합니다.
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경된 루트 상세 정보 반환"),
            @ApiResponse(responseCode = "400", description = "DELETED 상태로 변경 시도 또는 잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인 루트가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 루트",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RouteResponse> updateRouteStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UpdateRouteStatusRequest request) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(routeService.updateRouteStatus(userId, id, request.status()));
    }

    // ──────────────────────────────────────────────────────────
    // 즐겨찾기(저장)
    // ──────────────────────────────────────────────────────────

    @PostMapping("/{id}/save")
    @Operation(
            summary = "루트 즐겨찾기 저장",
            description = """
                    특정 루트를 즐겨찾기에 추가합니다. 같은 루트를 중복 저장할 수 없습니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Path Variable:**
                    - `id`: 저장할 루트의 ID

                    **저장 가능 조건:** 본인 루트이거나 공유 활성화된 루트(`isShared = true`)만 저장할 수 있습니다.
                    공유되지 않은 타인의 루트를 저장하려 하면 403을 반환합니다.
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "즐겨찾기 저장 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "공유되지 않은 타인의 루트",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 루트",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 저장된 루트",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> saveRoute(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Long userId = Long.parseLong(userDetails.getUsername());
        routeService.saveRoute(userId, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/save")
    @Operation(
            summary = "루트 즐겨찾기 취소",
            description = """
                    즐겨찾기에서 루트를 제거합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Path Variable:**
                    - `id`: 즐겨찾기를 취소할 루트의 ID

                    저장되지 않은 루트인 경우 404를 반환합니다.
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "즐겨찾기 취소 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "저장되지 않은 루트 또는 존재하지 않는 루트",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> unsaveRoute(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Long userId = Long.parseLong(userDetails.getUsername());
        routeService.unsaveRoute(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/saved")
    @Operation(
            summary = "저장한 루트 목록 조회",
            description = """
                    내가 즐겨찾기에 저장한 루트 목록을 반환합니다. 저장 최신순으로 정렬됩니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Response:** 루트 요약 목록 (경유지 미포함)
                    - `uuid`, `title`, `status`, `isShared`
                    - `totalDistance`, `estimatedTime`, `thumbnailUrl`, `createdAt`
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "저장한 루트 목록 반환"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<RouteSummaryResponse>> getSavedRoutes(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(routeService.getSavedRoutes(userId));
    }

    // ──────────────────────────────────────────────────────────
    // 공유
    // ──────────────────────────────────────────────────────────

    @PostMapping("/{id}/share")
    @Operation(
            summary = "루트 공유 활성화",
            description = """
                    루트 공유를 활성화합니다 (isShared = true).
                    이미 공유 중인 루트에 재호출해도 정상 처리됩니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Path Variable:**
                    - `id`: 공유할 루트의 ID

                    본인 루트가 아니면 403을 반환합니다.
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "공유 활성화 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인 루트가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 루트",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> enableSharing(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Long userId = Long.parseLong(userDetails.getUsername());
        routeService.enableSharing(userId, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/share")
    @Operation(
            summary = "루트 공유 비활성화",
            description = """
                    루트 공유를 비활성화합니다 (isShared = false).
                    비활성화 후 공유 루트 목록에서 제외됩니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Path Variable:**
                    - `id`: 공유를 비활성화할 루트의 ID

                    본인 루트가 아니면 403을 반환합니다.
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "공유 비활성화 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인 루트가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 루트",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> disableSharing(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Long userId = Long.parseLong(userDetails.getUsername());
        routeService.disableSharing(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/shared")
    @Operation(
            summary = "공유된 루트 목록 조회",
            description = """
                    isShared가 true인 루트 목록을 조회합니다. 인증 없이 접근 가능합니다.

                    **헤더:** 없음 (인증 불필요)

                    **Response:** 공유 루트 요약 목록
                    - `uuid`: 루트 UUID
                    - `title`: 루트 이름
                    - `description`: 루트 설명
                    - `status`: 상태 (DRAFT | PUBLISHED)
                    - `totalDistance`: 총 거리 (km)
                    - `estimatedTime`: 예상 소요시간 (분)
                    - `thumbnailUrl`: 대표 이미지 URL
                    - `name`: 첫 번째 경유지 이름
                    - `authorUuid`: 루트 작성자 UUID
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공유 루트 목록 반환 (빈 배열이면 공유된 루트 없음)")
    })
    public ResponseEntity<List<SharedRouteSummaryResponse>> getPublicSharedRoutes() {
        return ResponseEntity.ok(routeService.getPublicSharedRoutes());
    }

    @GetMapping("/shared/{uuid}")
    @Operation(
            summary = "공유된 루트 상세 조회",
            description = """
                    UUID로 공유된 루트의 상세 정보와 경유지 목록을 조회합니다. 인증 없이 접근 가능합니다.
                    isShared가 false이거나 존재하지 않는 UUID인 경우 404를 반환합니다.

                    **헤더:** 없음 (인증 불필요)

                    **Path Variable:**
                    - `uuid`: 조회할 루트의 UUID

                    **Response:** 루트 상세 정보 + 경유지 목록
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공유 루트 상세 정보 반환"),
            @ApiResponse(responseCode = "404", description = "존재하지 않거나 공유되지 않은 루트",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<RouteResponse> getPublicSharedRoute(@PathVariable String uuid) {
        return ResponseEntity.ok(routeService.getPublicSharedRoute(uuid));
    }

    @GetMapping("/sharing")
    @Operation(
            summary = "내가 공유 중인 루트 목록 조회",
            description = """
                    내가 공유 활성화한 루트 목록을 반환합니다. 삭제된 루트는 포함되지 않습니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Response:** 루트 요약 목록 (경유지 미포함)
                    - `uuid`, `title`, `status`, `isShared`
                    - `totalDistance`, `estimatedTime`, `thumbnailUrl`, `createdAt`
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공유 중인 루트 목록 반환"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<RouteSummaryResponse>> getSharingRoutes(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(routeService.getSharingRoutes(userId));
    }
}
