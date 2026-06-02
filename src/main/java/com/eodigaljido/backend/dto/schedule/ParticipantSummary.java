package com.eodigaljido.backend.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "코스 약속 참여자 요약")
public record ParticipantSummary(
        @Schema(description = "사용자 UUID", example = "u1")
        String uuid,

        @Schema(description = "사용자 닉네임", example = "지민")
        String nickname,

        @Schema(description = "사용자 아이디", example = "jimin")
        String userId
) {}
