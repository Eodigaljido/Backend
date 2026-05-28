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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/course-schedules")
@RequiredArgsConstructor
@Tag(name = "CourseSchedule", description = "코스 약속 관리 API")
public class CourseScheduleController {

    private final CourseScheduleService courseScheduleService;

    @PostMapping
    @Operation(
            summary = "약속 생성",
            description = """
                    새 코스 약속을 생성합니다.

                    - `date`: `yyyy-MM-dd` 형식
                    - `time`: `HH:mm` 형식
                    - `chatRoomUuid`: 기준 채팅방 UUID (선택). 입력 시 해당 채팅방 멤버를 참여자로 스냅샷 저장
                    - `chatRoomUuid` 미입력 시 생성자 본인만 참여자로 등록
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "약속 생성 성공",
                    content = @Content(schema = @Schema(implementation = CourseScheduleResponse.class))),
            @ApiResponse(responseCode = "400", description = "필수값 누락 또는 형식 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "채팅방 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "본인이 참여하지 않은 채팅방",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CourseScheduleResponse> createSchedule(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid CreateCourseScheduleRequest req) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.status(201).body(courseScheduleService.createSchedule(userId, req));
    }

    @GetMapping
    @Operation(
            summary = "약속 목록 조회",
            description = """
                    내 약속 목록을 조회합니다. `from`/`to` 파라미터로 날짜 범위를 지정할 수 있습니다.

                    - `from`: `yyyy-MM-dd` (포함)
                    - `to`: `yyyy-MM-dd` (포함)
                    - 기본 정렬: `scheduledAt ASC`
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CourseScheduleListResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CourseScheduleListResponse> getSchedules(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "시작 날짜 (yyyy-MM-dd)") @RequestParam(required = false) String from,
            @Parameter(description = "종료 날짜 (yyyy-MM-dd, 해당 날짜 포함)") @RequestParam(required = false) String to,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(courseScheduleService.getSchedules(userId, from, to, page, size));
    }

    @GetMapping("/nearest")
    @Operation(
            summary = "가장 가까운 약속 조회 (홈 카드용)",
            description = """
                    현재 시각 이후 가장 빠른 약속 1건을 반환합니다.

                    - 약속이 없으면 `204 No Content` 반환
                    - D-day 계산은 클라이언트에서 `scheduledAt` 기준으로 처리
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "약속 있음",
                    content = @Content(schema = @Schema(implementation = CourseScheduleNearestResponse.class))),
            @ApiResponse(responseCode = "204", description = "약속 없음"),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CourseScheduleNearestResponse> getNearestSchedule(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        CourseScheduleNearestResponse result = courseScheduleService.getNearestSchedule(userId);
        if (result == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{scheduleId}")
    @Operation(
            summary = "약속 상세 조회",
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CourseScheduleResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인 약속이 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "약속 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CourseScheduleResponse> getSchedule(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String scheduleId) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(courseScheduleService.getSchedule(userId, scheduleId));
    }

    @PatchMapping("/{scheduleId}")
    @Operation(
            summary = "약속 수정",
            description = """
                    약속 정보를 부분 수정합니다. 보내지 않은 필드는 변경되지 않습니다.

                    **chatRoomUuid 변경 정책:**
                    - 새 UUID 입력 시: 해당 채팅방 멤버로 참여자 재스냅샷
                    - 빈 문자열(`""`) 입력 시: 채팅방 연결 해제 후 참여자 초기화 (생성자만 유지)
                    - `null` 또는 필드 미전송 시: 변경 없음
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = CourseScheduleResponse.class))),
            @ApiResponse(responseCode = "400", description = "형식 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인 약속이 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "약속 또는 채팅방 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "본인이 참여하지 않은 채팅방",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CourseScheduleResponse> updateSchedule(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String scheduleId,
            @RequestBody @Valid UpdateCourseScheduleRequest req) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(courseScheduleService.updateSchedule(userId, scheduleId, req));
    }

    @DeleteMapping("/{scheduleId}")
    @Operation(
            summary = "약속 삭제",
            description = "소프트 딜리트 방식으로 약속을 삭제합니다.",
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "본인 약속이 아님",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "약속 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteSchedule(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String scheduleId) {
        Long userId = Long.valueOf(userDetails.getUsername());
        courseScheduleService.deleteSchedule(userId, scheduleId);
        return ResponseEntity.noContent().build();
    }
}
