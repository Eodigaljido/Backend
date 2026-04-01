package com.eodigaljido.backend.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "채팅방 생성 요청")
public record CreateChatRoomRequest(
        @Schema(description = "초대할 멤버의 UUID 목록 (본인 UUID 포함 시 자동 제외)", requiredMode = Schema.RequiredMode.REQUIRED,
                example = "[\"550e8400-e29b-41d4-a716-446655440002\"]")
        @NotEmpty List<@NotBlank String> memberUuids,

        @Schema(description = "채팅방 이름 (최대 100자, 미입력 시 멤버 닉네임 조합으로 자동 생성)", example = "우리팀 채팅방",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 100, message = "채팅방 이름은 100자를 초과할 수 없습니다.") String name
) {}
