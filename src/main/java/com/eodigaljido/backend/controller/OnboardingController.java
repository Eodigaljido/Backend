package com.eodigaljido.backend.controller;

import com.eodigaljido.backend.dto.common.ErrorResponse;
import com.eodigaljido.backend.dto.onboarding.*;
import com.eodigaljido.backend.service.OnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/onboarding")
@RequiredArgsConstructor
@Tag(name = "Onboarding", description = "온보딩 설문 API")
public class OnboardingController {

    private final OnboardingService onboardingService;

    @GetMapping("/status")
    @Operation(
            summary = "온보딩 상태 확인",
            description = """
                    현재 유저의 온보딩 완료 여부 및 진행 단계를 조회합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **응답:**
                    - `status`: NOT_STARTED / IN_PROGRESS / SKIPPED / COMPLETED
                    - `completed`: 온보딩 완료 여부
                    - `currentStep`: 현재 완료된 스텝 (0~4)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "온보딩 상태 반환"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OnboardingStatusResponse> getStatus(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                onboardingService.getStatus(Long.valueOf(userDetails.getUsername())));
    }

    @PostMapping("/skip")
    @Operation(
            summary = "온보딩 건너뛰기",
            description = """
                    온보딩 설문을 나중으로 미룹니다. (skipped 상태로 저장)

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "온보딩 건너뛰기 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 온보딩을 완료한 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> skip(
            @AuthenticationPrincipal UserDetails userDetails) {
        onboardingService.skip(Long.valueOf(userDetails.getUsername()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/questions")
    @Operation(
            summary = "설문 질문 목록 조회",
            description = """
                    4단계 설문의 질문 및 선택지 전체 목록을 조회합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "질문 목록 반환"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OnboardingQuestionsResponse> getQuestions() {
        return ResponseEntity.ok(onboardingService.getQuestions());
    }

    @PostMapping("/answers/step")
    @Operation(
            summary = "단계별 임시 저장",
            description = """
                    현재 스텝 답변을 임시 저장합니다. (중간 이탈 대비)

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Request Body:**
                    - `step` (필수): 현재 스텝 번호 (1~4)
                    - `answer` (필수): 선택한 답변
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "임시 저장 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 온보딩을 완료한 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> saveStep(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody OnboardingStepAnswerRequest request) {
        onboardingService.saveStep(Long.valueOf(userDetails.getUsername()), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/submit")
    @Operation(
            summary = "설문 최종 제출",
            description = """
                    4단계 모든 답변을 한번에 제출하고 추천 루트 3개를 수신합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Request Body:**
                    - `region` (필수): 거주 지역
                    - `age` (필수): 나이대
                    - `activity` (필수): 좋아하는 활동
                    - `gender` (필수): 성별
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "제출 성공 및 추천 루트 반환"),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 온보딩을 완료한 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OnboardingSubmitResponse> submit(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody OnboardingSubmitRequest request) {
        return ResponseEntity.ok(
                onboardingService.submit(Long.valueOf(userDetails.getUsername()), request));
    }

    @DeleteMapping("/answers")
    @Operation(
            summary = "설문 재설정 - 기존 답변 초기화",
            description = """
                    기존 설문 답변을 초기화하고 재설문을 시작합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "초기화 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "온보딩 기록 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> resetAnswers(
            @AuthenticationPrincipal UserDetails userDetails) {
        onboardingService.resetAnswers(Long.valueOf(userDetails.getUsername()));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/answers")
    @Operation(
            summary = "설문 재설정 - 특정 항목만 수정",
            description = """
                    완료된 온보딩 답변 중 특정 항목만 부분 수정합니다. (전체 재설문 없이)

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Request Body:** (변경할 항목만 포함)
                    - `region` (선택): 거주 지역
                    - `age` (선택): 나이대
                    - `activity` (선택): 좋아하는 활동
                    - `gender` (선택): 성별
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "완료되지 않은 온보딩은 수정 불가",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "온보딩 기록 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> patchAnswers(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody OnboardingPatchRequest request) {
        onboardingService.patchAnswers(Long.valueOf(userDetails.getUsername()), request);
        return ResponseEntity.noContent().build();
    }
}
