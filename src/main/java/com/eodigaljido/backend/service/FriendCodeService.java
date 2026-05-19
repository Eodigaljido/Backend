package com.eodigaljido.backend.service;

import com.eodigaljido.backend.domain.user.User;
import com.eodigaljido.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class FriendCodeService {

    private static final char[] CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final int CODE_LENGTH = 6;
    private static final int MAX_ATTEMPTS = 100;

    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();

    public String assignIfMissing(User user) {
        if (user.getFriendCode() != null && !user.getFriendCode().isBlank()) {
            return user.getFriendCode();
        }
        String code = generateUniqueCode();
        user.assignFriendCode(code);
        return code;
    }

    public String generateUniqueCode() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            String code = generateCode();
            if (!userRepository.existsByFriendCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("친구 코드를 생성할 수 없습니다.");
    }

    private String generateCode() {
        while (true) {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            boolean hasLetter = false;
            boolean hasDigit = false;
            for (int i = 0; i < CODE_LENGTH; i++) {
                char c = CHARS[random.nextInt(CHARS.length)];
                sb.append(c);
                hasLetter |= c >= 'A' && c <= 'Z';
                hasDigit |= c >= '0' && c <= '9';
            }
            if (hasLetter && hasDigit) {
                return sb.toString();
            }
        }
    }
}
