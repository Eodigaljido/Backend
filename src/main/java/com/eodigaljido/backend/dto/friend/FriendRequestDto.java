package com.eodigaljido.backend.dto.friend;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;

public record FriendRequestDto(
        String targetUuid,

        @Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d)[A-Z0-9]{6}$",
                message = "친구 코드는 대문자 영어와 숫자가 섞인 6자리여야 합니다.")
        String friendCode
) {
    @AssertTrue(message = "targetUuid 또는 friendCode 중 하나는 필수입니다.")
    public boolean hasTarget() {
        return (targetUuid != null && !targetUuid.isBlank())
                || (friendCode != null && !friendCode.isBlank());
    }
}
