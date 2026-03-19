package com.eodigaljido.backend.controller;

import com.eodigaljido.backend.dto.user.*;
import com.eodigaljido.backend.service.AuthService;
import com.eodigaljido.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
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
    public ResponseEntity<MyProfileResponse> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getMyProfile(Long.parseLong(userDetails.getUsername())));
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
    public ResponseEntity<Void> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        userService.updateProfile(Long.parseLong(userDetails.getUsername()), request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    @Operation(
            summary = "회원 탈퇴 (soft delete)",
            description = """
                    계정을 비활성화합니다. 데이터는 보존되며 복구가 불가능합니다.

                    **헤더:** `Authorization: Bearer {accessToken}` (필수)
                    """
    )
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal UserDetails userDetails) {
        userService.withdraw(Long.parseLong(userDetails.getUsername()));
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
    public ResponseEntity<Void> updateProfileImage(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileImageRequest request) {
        userService.updateProfileImage(Long.parseLong(userDetails.getUsername()), request);
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
    public ResponseEntity<Void> deleteProfileImage(
            @AuthenticationPrincipal UserDetails userDetails) {
        userService.deleteProfileImage(Long.parseLong(userDetails.getUsername()));
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
    public ResponseEntity<Void> updatePhone(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdatePhoneRequest request) {
        authService.updatePhone(Long.parseLong(userDetails.getUsername()), request.phone());
        return ResponseEntity.noContent().build();
    }
}
