package com.eodigaljido.backend.controller;

import com.eodigaljido.backend.dto.user.UpdatePhoneRequest;
import com.eodigaljido.backend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "유저 API")
public class UserController {

    private final AuthService authService;

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
    ResponseEntity<Void> updatePhone(@AuthenticationPrincipal UserDetails userDetails,
                                     @Valid @RequestBody UpdatePhoneRequest request) {
        authService.updatePhone(Long.parseLong(userDetails.getUsername()), request.phone());
        return ResponseEntity.noContent().build();
    }
}
