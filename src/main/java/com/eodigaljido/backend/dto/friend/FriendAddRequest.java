package com.eodigaljido.backend.dto.friend;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "친구 코드로 친구 추가 요청")
public record FriendAddRequest(
        @Schema(description = "추가할 상대방의 친구 코드 (대문자 영어+숫자 6자리)", example = "ESSP3P")
        @NotBlank(message = "friendCode는 필수입니다.")
        @Size(min = 6, max = 6, message = "친구 코드는 6자리여야 합니다.")
        String friendCode
) {}
