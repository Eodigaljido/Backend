package com.eodigaljido.backend.controller;

import com.eodigaljido.backend.dto.common.ErrorResponse;
import com.eodigaljido.backend.dto.notification.NotificationResponse;
import com.eodigaljido.backend.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "알림 API")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(
            summary = "알림 목록 조회",
            description = """
                    최신순으로 10개씩 반환합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Query Parameter:**
                    - `page` (선택): 페이지 번호, 0부터 시작. 생략 시 0 (첫 로드)
                      - `page=0`: 최신 10개
                      - `page=1`: 그 다음 10개 (더 보기)
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "알림 목록 반환 (빈 배열이면 알림 없음)"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(notificationService.getNotifications(userId, page));
    }

    @GetMapping("/unread-count")
    @Operation(
            summary = "읽지 않은 알림 수 조회",
            description = """
                    읽지 않은 알림의 개수를 숫자로 반환합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "읽지 않은 알림 수 반환"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Long> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }

    @PatchMapping("/{id}/read")
    @Operation(
            summary = "단건 읽음 처리",
            description = """
                    특정 알림 하나를 읽음 상태로 변경합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Path Variable:**
                    - `id`: 읽음 처리할 알림 ID (알림 목록 조회 응답의 `id` 값)
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "읽음 처리된 알림 반환"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인 알림이 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 알림",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<NotificationResponse> markAsRead(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(notificationService.markAsRead(userId, id));
    }

    @PatchMapping("/read")
    @Operation(
            summary = "전체 읽음 처리",
            description = """
                    읽지 않은 알림을 모두 읽음 상태로 변경합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "전체 읽음 처리 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }
}
