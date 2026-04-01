package com.eodigaljido.backend.controller;

import com.eodigaljido.backend.dto.common.ErrorResponse;
import com.eodigaljido.backend.dto.user.*;
import com.eodigaljido.backend.service.AuthService;
import com.eodigaljido.backend.service.UserService;
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

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "회원 / 프로필 API")
public class UserController {

    private final AuthService authService;
    private final UserService userService;

    @GetMapping("/me")
    @Operation(
            summary = "내 프로필 정보 전체 조회",
            description = """
                    로그인한 사용자의 프로필 전체 정보를 조회합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "프로필 정보 반환"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "410", description = "이미 탈퇴한 계정",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<MyProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getMyProfile(Long.valueOf(userDetails.getUsername())));
    }

    @PatchMapping("/me")
    @Operation(
            summary = "닉네임 / 자기소개 수정",
            description = """
                    닉네임 또는 자기소개를 수정합니다. 변경할 필드만 포함하면 됩니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Request Body:**
                    - `nickname` (선택): 변경할 닉네임 (2~20자)
                    - `bio` (선택): 변경할 자기소개 (255자 이하)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않음 (닉네임 길이 등)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 닉네임",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "410", description = "이미 탈퇴한 계정",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        userService.updateProfile(Long.valueOf(userDetails.getUsername()), request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    @Operation(
            summary = "회원 탈퇴 (soft delete)",
            description = """
                    계정을 비활성화합니다. 데이터는 보존되며 재가입은 불가능합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "회원 탈퇴 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "410", description = "이미 탈퇴한 계정",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal UserDetails userDetails) {
        userService.withdraw(Long.valueOf(userDetails.getUsername()));
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/profile-image")
    @Operation(
            summary = "프로필 이미지 변경",
            description = """
                    프로필 이미지 URL을 변경합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Request Body:**
                    - `profileImageUrl` (필수): 새 프로필 이미지 URL (512자 이하)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "이미지 변경 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않음 (URL 누락 또는 길이 초과)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "410", description = "이미 탈퇴한 계정",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> updateProfileImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileImageRequest request) {
        userService.updateProfileImage(Long.valueOf(userDetails.getUsername()), request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me/profile-image")
    @Operation(
            summary = "프로필 이미지 삭제 (기본 이미지로 변경)",
            description = """
                    프로필 이미지를 제거하고 기본 이미지로 되돌립니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "기본 이미지로 초기화 성공"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "410", description = "이미 탈퇴한 계정",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteProfileImage(
            @AuthenticationPrincipal UserDetails userDetails) {
        userService.deleteProfileImage(Long.valueOf(userDetails.getUsername()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    @Operation(
            summary = "유저 검색 (닉네임)",
            description = """
                    닉네임으로 유저를 검색합니다. 부분 일치를 지원합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Query Parameter:**
                    - `keyword` (필수): 검색할 닉네임 키워드
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검색 결과 반환 (빈 배열이면 결과 없음)"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<UserSearchResponse>> searchUsers(
            @RequestParam String keyword) {
        return ResponseEntity.ok(userService.searchUsers(keyword));
    }

    @GetMapping("/{uuid}")
    @Operation(
            summary = "다른 유저 프로필 조회",
            description = """
                    UUID로 다른 유저의 공개 프로필을 조회합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **Path Variable:**
                    - `uuid`: 조회할 유저의 UUID
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "유저 프로필 반환"),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않거나 탈퇴한 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserProfileResponse> getUserProfile(
            @PathVariable String uuid) {
        return ResponseEntity.ok(userService.getUserProfile(uuid));
    }

    @PatchMapping("/me/phone")
    @Operation(
            summary = "전화번호 변경",
            description = """
                    로그인한 사용자의 전화번호를 변경합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)

                    **사전 조건:** 요청 전에 아래 두 단계를 반드시 완료해야 합니다.
                    1. `POST /auth/phone/code` — 변경할 번호로 phone, purpose=CHANGE_PHONE 로 인증번호 발송
                    2. `POST /auth/phone/verify` — 수신한 6자리 코드 검증 (purpose=CHANGE_PHONE)

                    인증 완료 후 10분 이내에 본 API를 호출해야 합니다.

                    **Request Body:**
                    - `phone` (필수): 변경할 새 휴대폰 번호 (하이픈 없이, 예: 01098765432), 인증 완료된 번호와 일치해야 함
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "전화번호 변경 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값이 올바르지 않음 또는 전화번호 미인증",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 전화번호",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> updatePhone(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdatePhoneRequest request) {
        authService.updatePhone(Long.valueOf(userDetails.getUsername()), request.phone());
        return ResponseEntity.noContent().build();
    }
}
