package com.eodigaljido.backend.exception;

import com.eodigaljido.backend.dto.common.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    ResponseEntity<ErrorResponse> handleAuth(AuthException e) {
        log.warn("[Auth 오류] {} (status={})", e.getMessage(), e.getStatus().value());
        return ResponseEntity.status(e.getStatus())
                .body(new ErrorResponse(e.getStatus().value(), e.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(PhoneVerificationException.class)
    ResponseEntity<ErrorResponse> handlePhoneVerification(PhoneVerificationException e) {
        log.warn("[전화번호 인증 오류] {} (status={})", e.getMessage(), e.getStatus().value());
        return ResponseEntity.status(e.getStatus())
                .body(new ErrorResponse(e.getStatus().value(), e.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(FriendException.class)
    ResponseEntity<ErrorResponse> handleFriend(FriendException e) {
        log.warn("[친구 오류] {} (status={})", e.getMessage(), e.getStatus().value());
        return ResponseEntity.status(e.getStatus())
                .body(new ErrorResponse(e.getStatus().value(), e.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(ChatException.class)
    ResponseEntity<ErrorResponse> handleChat(ChatException e) {
        log.warn("[채팅 오류] {} (status={})", e.getMessage(), e.getStatus().value());
        return ResponseEntity.status(e.getStatus())
                .body(new ErrorResponse(e.getStatus().value(), e.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(UserException.class)
    ResponseEntity<ErrorResponse> handleUser(UserException e) {
        log.warn("[유저 오류] {} (status={})", e.getMessage(), e.getStatus().value());
        return ResponseEntity.status(e.getStatus())
                .body(new ErrorResponse(e.getStatus().value(), e.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(OnboardingException.class)
    ResponseEntity<ErrorResponse> handleOnboarding(OnboardingException e) {
        log.warn("[온보딩 오류] {} (status={})", e.getMessage(), e.getStatus().value());
        return ResponseEntity.status(e.getStatus())
                .body(new ErrorResponse(e.getStatus().value(), e.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + (fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "유효하지 않은 값"))
                .collect(Collectors.joining(", "));
        if (message.isBlank()) message = "유효하지 않은 요청입니다.";
        log.warn("[유효성 검사 실패] {}", message);
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, message, LocalDateTime.now()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleGeneral(Exception e) {
        log.error("[서버 내부 오류] {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "서버 오류가 발생했습니다.", LocalDateTime.now()));
    }
}
