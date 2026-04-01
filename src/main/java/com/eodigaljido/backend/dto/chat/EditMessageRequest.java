package com.eodigaljido.backend.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "메시지 수정 요청")
public record EditMessageRequest(
        @Schema(description = "수정할 메시지 내용 (최대 2000자)", example = "수정된 내용입니다.", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 2000) String content
) {}
