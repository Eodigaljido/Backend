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
    @Operation(summary = "전화번호 등록/변경", description = "로그인 상태에서 전화번호 등록 또는 변경 (사전 인증 필요)")
    ResponseEntity<Void> updatePhone(@AuthenticationPrincipal UserDetails userDetails,
                                     @Valid @RequestBody UpdatePhoneRequest request) {
        authService.updatePhone(Long.parseLong(userDetails.getUsername()), request.phone());
        return ResponseEntity.noContent().build();
    }
}
