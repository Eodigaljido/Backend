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
@Tag(name = "CourseSchedule", description = "Course schedule APIs")
public class CourseScheduleController {

    private final CourseScheduleService courseScheduleService;

    @GetMapping
    @Operation(
            summary = "List course schedules",
            description = "Returns schedules created by the requester or attached to chat rooms where the requester is a member.",
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Schedule list",
                    content = @Content(schema = @Schema(implementation = CourseScheduleListResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CourseScheduleListResponse> getSchedules(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Inclusive start date, yyyy-MM-dd, interpreted as KST")
            @RequestParam(required = false) String from,
            @Parameter(description = "Inclusive end date, yyyy-MM-dd, interpreted as KST")
            @RequestParam(required = false) String to,
            @Parameter(description = "Filter by chat room UUID")
            @RequestParam(required = false) String chatRoomUuid,
            @Parameter(description = "When true, only returns schedules at or after now")
            @RequestParam(defaultValue = "false") boolean upcomingOnly) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(
                courseScheduleService.getSchedules(userId, from, to, chatRoomUuid, upcomingOnly));
    }

    @GetMapping("/nearest")
    @Operation(
            summary = "Get nearest upcoming course schedule",
            security = @SecurityRequirement(name = "Bearer")
    )
    public ResponseEntity<CourseScheduleNearestResponse> getNearestSchedule(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(courseScheduleService.getNearestSchedule(userId));
    }

    @GetMapping("/{scheduleUuid}")
    @Operation(
            summary = "Get course schedule detail",
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Schedule detail",
                    content = @Content(schema = @Schema(implementation = CourseScheduleResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Schedule not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CourseScheduleResponse> getSchedule(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String scheduleUuid) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(courseScheduleService.getSchedule(userId, scheduleUuid));
    }

    @PostMapping
    @Operation(
            summary = "Create course schedule",
            description = "The requester must be a member of the selected chat room.",
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Schedule created",
                    content = @Content(schema = @Schema(implementation = CourseScheduleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Not a chat room member",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Chat room or course not found",
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
            summary = "Update course schedule",
            description = "The creator or the chat room admin can update a schedule.",
            security = @SecurityRequirement(name = "Bearer")
    )
    public ResponseEntity<CourseScheduleResponse> updateSchedule(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String scheduleUuid,
            @RequestBody @Valid UpdateCourseScheduleRequest req) {
        Long userId = Long.valueOf(userDetails.getUsername());
        return ResponseEntity.ok(courseScheduleService.updateSchedule(userId, scheduleUuid, req));
    }

    @DeleteMapping("/{scheduleUuid}")
    @Operation(
            summary = "Delete course schedule",
            description = "The creator or the chat room admin can delete a schedule.",
            security = @SecurityRequirement(name = "Bearer")
    )
    public ResponseEntity<Void> deleteSchedule(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String scheduleUuid) {
        Long userId = Long.valueOf(userDetails.getUsername());
        courseScheduleService.deleteSchedule(userId, scheduleUuid);
        return ResponseEntity.noContent().build();
    }
}
