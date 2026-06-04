package com.eodigaljido.backend.dto.course;

import com.eodigaljido.backend.domain.route.RouteTag;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "내 루트 생성/수정 요청")
public record CreateMyCourseRequest(
        @NotBlank
        @Size(max = 100)
        @Schema(description = "루트 이름", example = "서울 고궁 투어", requiredMode = Schema.RequiredMode.REQUIRED)
        String title,

        @Schema(description = "공동 편집 여부. true면 요청자가 OWNER로 등록됩니다.", example = "true")
        Boolean collaborative,

        @Schema(description = "PATCH 충돌 방지를 위한 현재 버전", example = "12")
        Long version,

        @Schema(description = "경유지 목록")
        List<@Valid StopRequest> stops,

        @Schema(description = "이동 구간 목록")
        List<LegRequest> legs,

        @Size(max = 2, message = "태그는 최대 2개까지 입력 가능합니다.")
        @ArraySchema(
                arraySchema = @Schema(description = "태그 목록. 생략하면 기존 태그를 유지하고, 빈 배열이면 모두 제거합니다."),
                schema = @Schema(implementation = RouteTag.class)
        )
        List<RouteTag> tags
) {}
