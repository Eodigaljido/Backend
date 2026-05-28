package com.eodigaljido.backend.dto.schedule;

import jakarta.validation.constraints.Size;

public record UpdateCourseScheduleRequest(
    @Size(min = 1, max = 80, message = "약속 이름은 1~80자 이내여야 합니다.")
    String title,

    String date,

    String time,

    String chatRoomUuid,

    @Size(max = 300, message = "메모는 300자 이내여야 합니다.")
    String memo
) {}
