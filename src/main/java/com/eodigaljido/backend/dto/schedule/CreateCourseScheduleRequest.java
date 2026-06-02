package com.eodigaljido.backend.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

@Schema(description = "코스 약속 생성 요청")
public record CreateCourseScheduleRequest(
        @Schema(description = "약속 이름. 앞뒤 공백은 제거되며 최대 100자입니다.", example = "주말 카페 코스")
        @NotBlank
        @Size(max = 100)
        String title,

        @Schema(description = "약속 일시. ISO-8601 형식이며 오프셋 포함 값을 권장합니다.", example = "2026-06-08T19:00:00+09:00")
        @NotNull
        OffsetDateTime scheduledAt,

        @Schema(description = "약속을 연결할 기존 채팅방 UUID. 요청 사용자는 해당 채팅방 멤버여야 합니다.", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        @NotNull
        String chatRoomUuid,

        @Schema(description = "연결할 코스 UUID. 없으면 null입니다.", nullable = true, example = "7ecc5401-1234-5678-abcd-000000000001")
        String courseUuid,

        @Schema(description = "true이면 약속 생성 후 채팅방에 시스템 메시지를 남깁니다.", example = "false", defaultValue = "false")
        Boolean notifyChat
) {}
