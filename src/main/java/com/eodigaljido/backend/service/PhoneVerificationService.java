package com.eodigaljido.backend.service;

import com.eodigaljido.backend.domain.user.PhoneVerification;
import com.eodigaljido.backend.exception.PhoneVerificationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PhoneVerificationService {

    private static final int CODE_EXPIRY_MINUTES = 3;
    private static final int VERIFIED_EXPIRY_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;

    private final StringRedisTemplate redisTemplate;
    private final SolapiService solapiService;

    // ── 코드 발송 ─────────────────────────────────────────────────────────────

    public void sendCode(String phone, PhoneVerification.Purpose purpose) {
        String code = generateCode();
        redisTemplate.opsForValue().set(codeKey(phone, purpose), code, CODE_EXPIRY_MINUTES, TimeUnit.MINUTES);
        redisTemplate.delete(attemptsKey(phone, purpose)); // 이전 시도 횟수 초기화
        solapiService.sendSms(phone, "[어디갈지도] 인증번호: " + code + " (3분 내 입력)");
    }

    // ── 코드 검증 ─────────────────────────────────────────────────────────────

    public void verifyCode(String phone, String code, PhoneVerification.Purpose purpose) {
        String storedCode = redisTemplate.opsForValue().get(codeKey(phone, purpose));
        if (storedCode == null) {
            throw new PhoneVerificationException(
                    "인증번호가 만료되었거나 존재하지 않습니다. 다시 요청해주세요.", HttpStatus.NOT_FOUND);
        }

        // 시도 횟수 초과 확인
        String attemptsStr = redisTemplate.opsForValue().get(attemptsKey(phone, purpose));
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;
        if (attempts >= MAX_ATTEMPTS) {
            throw new PhoneVerificationException(
                    "인증 시도 횟수를 초과했습니다. 인증번호를 다시 요청해주세요.", HttpStatus.TOO_MANY_REQUESTS);
        }

        // 시도 횟수 증가
        redisTemplate.opsForValue().increment(attemptsKey(phone, purpose));
        redisTemplate.expire(attemptsKey(phone, purpose), CODE_EXPIRY_MINUTES, TimeUnit.MINUTES);

        if (!storedCode.equals(code)) {
            throw new PhoneVerificationException("인증번호가 올바르지 않습니다.", HttpStatus.BAD_REQUEST);
        }

        // 인증 성공 → 코드/시도횟수 삭제, 인증 완료 플래그 저장
        redisTemplate.delete(codeKey(phone, purpose));
        redisTemplate.delete(attemptsKey(phone, purpose));
        redisTemplate.opsForValue().set(verifiedKey(phone, purpose), "true",
                VERIFIED_EXPIRY_MINUTES, TimeUnit.MINUTES);
    }

    // ── 인증 완료 여부 확인 ───────────────────────────────────────────────────

    public boolean checkVerified(String phone, PhoneVerification.Purpose purpose) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(verifiedKey(phone, purpose)));
    }

    public void clearVerified(String phone, PhoneVerification.Purpose purpose) {
        redisTemplate.delete(verifiedKey(phone, purpose));
    }

    // ── Redis 키 ──────────────────────────────────────────────────────────────

    private String codeKey(String phone, PhoneVerification.Purpose purpose) {
        return "phone:code:" + phone + ":" + purpose.name();
    }

    private String attemptsKey(String phone, PhoneVerification.Purpose purpose) {
        return "phone:attempts:" + phone + ":" + purpose.name();
    }

    private String verifiedKey(String phone, PhoneVerification.Purpose purpose) {
        return "phone:verified:" + phone + ":" + purpose.name();
    }

    private String generateCode() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }
}
