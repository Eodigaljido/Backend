package com.eodigaljido.backend.dto.group;

import com.eodigaljido.backend.domain.group.Group.GroupType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "모임 생성 요청")
public record CreateGroupRequest(
        @Schema(description = "모임 이름", example = "서울 산책 크루")
        @NotBlank @Size(max = 100)
        String name,

        @Schema(description = "모임 소개", example = "서울 곳곳을 함께 걷는 모임")
        String description,

        @Schema(description = "모임 프로필 이미지 URL")
        String profileImageUrl,

        @Schema(description = "공개 여부: PUBLIC | PRIVATE", example = "PUBLIC")
        GroupType type,

        @Schema(description = "가입 승인 필요 여부 (PUBLIC일 때 유효). false=바로 가입, true=승인 필요", example = "false")
        Boolean requiresApproval
) {}
