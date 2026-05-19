package com.eodigaljido.backend.config;

import com.eodigaljido.backend.repository.UserRepository;
import com.eodigaljido.backend.service.FriendCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class FriendCodeBackfillRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final FriendCodeService friendCodeService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var users = userRepository.findUsersMissingFriendCode();
        if (users.isEmpty()) {
            return;
        }
        users.forEach(friendCodeService::assignIfMissing);
        log.info("친구 코드가 없는 기존 사용자 {}명에게 친구 코드를 발급했습니다.", users.size());
    }
}
