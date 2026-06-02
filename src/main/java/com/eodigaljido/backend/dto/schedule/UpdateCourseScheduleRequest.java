package com.eodigaljido.backend.dto.schedule;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

@Schema(description = "코스 약속 수정 요청. 포함된 필드만 수정됩니다.")
public record UpdateCourseScheduleRequest(
        @Schema(description = "변경할 약속 이름. 최대 100자입니다.", nullable = true, example = "주말 카페 코스 시간 변경")
        @Size(max = 100)
        String title,

        @Schema(description = "변경할 약속 일시. ISO-8601 형식입니다.", nullable = true, example = "2026-06-08T20:00:00+09:00")
        OffsetDateTime scheduledAt,

        @Schema(description = "변경할 채팅방 UUID. 요청 사용자는 새 채팅방의 멤버여야 합니다.", nullable = true, example = "b1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String chatRoomUuid,

        @Schema(description = "변경할 연결 코스 UUID. 없으면 null입니다.", nullable = true, example = "7ecc5401-1234-5678-abcd-000000000001")
        String courseUuid
) {}
