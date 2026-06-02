package com.eodigaljido.backend.controller;

import com.eodigaljido.backend.dto.common.ErrorResponse;
import com.eodigaljido.backend.dto.schedule.CourseScheduleListResponse;
import com.eodigaljido.backend.dto.schedule.CourseScheduleNearestResponse;
import com.eodigaljido.backend.dto.schedule.CourseScheduleResponse;
import com.eodigaljido.backend.dto.schedule.CreateCourseScheduleRequest;
import com.eodigaljido.backend.dto.schedule.UpdateCourseScheduleRequest;
import com.eodigaljido.backend.service.CourseScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/course-schedules")
@RequiredArgsConstructor
@Tag(name = "CourseSchedule", description = "코스 약속 일정 관리 API")
public class CourseScheduleController {

    private final CourseScheduleService courseScheduleService;

    @GetMapping
    @Operation(
            summary = "코스 약속 목록 조회",
            description = """
                    로그인 사용자가 볼 수 있는 코스 약속 목록을 조회합니다.

                    조회 대상:
                    - 내가 직접 생성한 약속
                    - 내가 멤버로 참여 중인 채팅방에 연결된 약속

                    날짜 필터의 `from`, `to`는 `yyyy-MM-dd` 형식이며 KST 기준 하루 범위로 해석합니다.
                    응답은 `scheduledAt` 오름차순, 같은 시간이면 `createdAt` 오름차순으로 정렬됩니다.
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "코스 약속 목록 반환",
                    content = @Content(schema = @Schema(implementation = CourseScheduleListResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "chatRoomUuid 지정 시 해당 채팅방 멤버가 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "chatRoomUuid 지정 시 존재하지 않는 채팅방",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CourseScheduleListResponse> getSchedules(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "조회 시작일(포함), KST 기준 `yyyy-MM-dd`", example = "2026-06-01")
            @RequestParam(required = false) String from,
            @Parameter(description = "조회 종료일(포함), KST 기준 `yyyy-MM-dd`", example = "2026-06-30")
            @RequestParam(required = false) String to,
            @Parameter(description = "특정 채팅방의 약속만 조회할 때 사용하는 채팅방 UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
            @RequestParam(required = false) String chatRoomUuid,
            @Parameter(description = "`true`이면 현재 시각 이후의 약속만 조회", example = "true")
            @RequestParam(defaultValue = "false") boolean upcomingOnly) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(
                courseScheduleService.getSchedules(userId, from, to, chatRoomUuid, upcomingOnly));
    }

    @GetMapping("/nearest")
    @Operation(
            summary = "가장 가까운 코스 약속 조회",
            description = """
                    로그인 사용자가 볼 수 있는 미래 약속 중 가장 가까운 1건을 조회합니다.

                    홈 화면의 D-day 카드처럼 한 건만 필요한 화면에서 사용할 수 있습니다.
                    약속이 없으면 `item`이 `null`입니다.
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "가장 가까운 약속 반환. 약속이 없으면 item은 null",
                    content = @Content(schema = @Schema(implementation = CourseScheduleNearestResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CourseScheduleNearestResponse> getNearestSchedule(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(courseScheduleService.getNearestSchedule(userId));
    }

    @GetMapping("/{scheduleUuid}")
    @Operation(
            summary = "코스 약속 상세 조회",
            description = """
                    코스 약속 UUID로 상세 정보를 조회합니다.

                    생성자이거나 약속이 연결된 채팅방의 현재 멤버만 조회할 수 있습니다.
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "코스 약속 상세 반환",
                    content = @Content(schema = @Schema(implementation = CourseScheduleResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "해당 약속을 조회할 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않거나 삭제된 약속",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CourseScheduleResponse> getSchedule(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "코스 약속 UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String scheduleUuid) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(courseScheduleService.getSchedule(userId, scheduleUuid));
    }

    @PostMapping
    @Operation(
            summary = "코스 약속 생성",
            description = """
                    기존 채팅방에 연결되는 코스 약속을 생성합니다.

                    필수 조건:
                    - `title`, `scheduledAt`, `chatRoomUuid`는 필수입니다.
                    - 요청 사용자는 `chatRoomUuid`에 해당하는 채팅방의 현재 멤버여야 합니다.
                    - `courseUuid`는 선택 값이며, 공개 코스 또는 본인이 접근 가능한 내 코스만 연결할 수 있습니다.
                    - `notifyChat`이 `true`이면 해당 채팅방에 약속 생성 시스템 메시지를 남깁니다.
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "코스 약속 생성 성공",
                    content = @Content(schema = @Schema(implementation = CourseScheduleResponse.class))),
            @ApiResponse(responseCode = "400", description = "필수 값 누락 또는 요청 값 형식 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "채팅방 멤버가 아니거나 코스 접근 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방 또는 코스",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CourseScheduleResponse> createSchedule(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid CreateCourseScheduleRequest req) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.status(201).body(courseScheduleService.createSchedule(userId, req));
    }

    @PatchMapping("/{scheduleUuid}")
    @Operation(
            summary = "코스 약속 수정",
            description = """
                    코스 약속 정보를 부분 수정합니다. 요청 본문에 포함된 필드만 변경됩니다.

                    수정 권한:
                    - 약속 생성자
                    - 약속이 연결된 채팅방의 ADMIN 멤버

                    `chatRoomUuid`를 변경하는 경우, 요청 사용자가 새 채팅방의 멤버인지 다시 검증합니다.
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "코스 약속 수정 성공",
                    content = @Content(schema = @Schema(implementation = CourseScheduleResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 값 형식 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "수정 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 약속, 채팅방 또는 코스",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CourseScheduleResponse> updateSchedule(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "코스 약속 UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String scheduleUuid,
            @RequestBody @Valid UpdateCourseScheduleRequest req) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(courseScheduleService.updateSchedule(userId, scheduleUuid, req));
    }

    @DeleteMapping("/{scheduleUuid}")
    @Operation(
            summary = "코스 약속 삭제",
            description = """
                    코스 약속을 소프트 삭제합니다.

                    삭제 권한:
                    - 약속 생성자
                    - 약속이 연결된 채팅방의 ADMIN 멤버
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "코스 약속 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰 없음/만료",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "삭제 권한 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않거나 삭제된 약속",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteSchedule(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "코스 약속 UUID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable String scheduleUuid) {
        Long userId = Long.valueOf(userDetails.getUsername());
        courseScheduleService.deleteSchedule(userId, scheduleUuid);
        return ResponseEntity.noContent().build();
    }
}
