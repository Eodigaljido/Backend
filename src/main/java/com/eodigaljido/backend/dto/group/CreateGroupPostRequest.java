package com.eodigaljido.backend.dto.group;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "모임 게시물 작성 요청")
public record CreateGroupPostRequest(
        @Schema(description = "게시물 내용")
        @NotBlank
        String content
) {}
