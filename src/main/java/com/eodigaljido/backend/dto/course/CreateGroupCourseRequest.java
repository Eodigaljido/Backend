package com.eodigaljido.backend.dto.course;

import com.eodigaljido.backend.domain.route.RouteTag;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "모임 루트 생성 요청")
public record CreateGroupCourseRequest(
        @NotBlank
        @Size(max = 100)
        @Schema(description = "루트 이름 (최대 100자)", example = "한강 자전거 코스", requiredMode = Schema.RequiredMode.REQUIRED)
        String title,

        @NotBlank
        @Schema(description = "소속 모임 UUID", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
        String groupUuid,

        @Schema(description = "경유지 목록 (start → via → end 순서)")
        List<@Valid StopRequest> stops,

        @Schema(description = "이동 구간 목록 (stop[i]~stop[i+1] 사이의 이동 정보)")
        List<LegRequest> legs,

        @Size(max = 2, message = "태그는 최대 2개까지 입력 가능합니다.")
        @ArraySchema(
                arraySchema = @Schema(
                        description = "태그 목록 (선택, 최대 2개). 빈 배열(`[]`)을 전달하면 태그를 모두 삭제합니다.",
                        example = "[\"산책\", \"카페\"]"
                ),
                schema = @Schema(implementation = RouteTag.class)
        )
        List<RouteTag> tags,

        @Schema(description = "루트 전용 채팅방 자동 생성 여부 (기본값: true)", example = "true", defaultValue = "true")
        Boolean createChatRoom,

        @Schema(description = "루트 기록방을 종속시킬 부모 채팅방 UUID. 해당 모임 소속 채팅방이어야 하며, " +
                "createChatRoom이 true(기본값)일 때 필수입니다.",
                example = "550e8400-e29b-41d4-a716-446655440000")
        String chatRoomUuid
) {}
