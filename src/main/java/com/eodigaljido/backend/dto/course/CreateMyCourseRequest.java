package com.eodigaljido.backend.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "내 루트 생성/수정 요청")
public record CreateMyCourseRequest(
        @NotBlank
        @Size(max = 100)
        @Schema(description = "루트 이름 (최대 100자)", example = "서울 고궁 투어", requiredMode = Schema.RequiredMode.REQUIRED)
        String title,

        @Schema(description = "공동 편집 여부 (true면 isShared = true)", example = "false")
        Boolean collaborative,

        @Schema(description = "경유지 목록 (start → via → end 순서)")
        List<StopRequest> stops,

        @Schema(description = "이동 구간 목록 (stop[i]~stop[i+1] 사이의 이동 정보)")
        List<LegRequest> legs
) {}
