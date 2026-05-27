package com.eodigaljido.backend.controller;

import com.eodigaljido.backend.dto.course.CourseItemResponse;
import com.eodigaljido.backend.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
@Tag(name = "Home", description = "홈 화면 API")
public class HomeController {

    private final CourseService courseService;

    @GetMapping("/courses")
    @Operation(
            summary = "홈 인기 코스 목록 조회",
            description = """
                    홈 화면에서 표시할 인기 코스 목록을 반환합니다. 인증 없이 접근 가능합니다.
                    조회수(`views`) 기준 내림차순으로 정렬됩니다.
                    각 코스에는 `routeSteps`, `rating`, `reviewCount`, `departure`, `arrival` 등 홈 렌더링에 필요한 필드가 모두 포함됩니다.

                    로그인 시 **`savedByMe`** 필드가 정확하게 반환됩니다. 미로그인 시 `false`.
                    """,
            security = {}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "홈 코스 목록 반환",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = CourseItemResponse.class)))
            )
    })
    public ResponseEntity<List<CourseItemResponse>> getHomeCourses(
            @Parameter(description = "반환할 코스 수 (기본 10)", example = "10")
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userDetails != null ? Long.parseLong(userDetails.getUsername()) : null;
        return ResponseEntity.ok(courseService.getHomeCourses(limit, userId));
    }
}
