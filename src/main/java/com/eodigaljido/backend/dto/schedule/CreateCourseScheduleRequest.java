package com.eodigaljido.backend.dto.schedule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCourseScheduleRequest(
    @NotBlank(message = "약속 이름은 필수입니다.")
    @Size(min = 1, max = 80, message = "약속 이름은 1~80자 이내여야 합니다.")
    String title,

    @NotNull(message = "날짜는 필수입니다.")
    String date,

    @NotNull(message = "시간은 필수입니다.")
    String time,

    String chatRoomUuid,

    @Size(max = 300, message = "메모는 300자 이내여야 합니다.")
    String memo
) {}
