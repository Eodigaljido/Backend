package com.eodigaljido.backend.dto.course;

import com.eodigaljido.backend.domain.route.RouteWaypoint;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "경유지(stop) 응답")
public record StopResponse(
        @Schema(description = "순서 (1부터 시작)", example = "1")
        int sequence,

        @Schema(description = "경유지 구분 (start | via | end)", example = "start")
        String kind,

        @Schema(description = "경유지 이름", example = "경복궁")
        String title,

        @Schema(description = "일정 시각 문자열", example = "09:00")
        String timeLine,

        @Schema(description = "위도", example = "37.5796212")
        BigDecimal lat,

        @Schema(description = "경도", example = "126.9770162")
        BigDecimal lng
) {
    public static StopResponse from(RouteWaypoint w) {
        return new StopResponse(w.getSequence(), w.getKind(), w.getName(), w.getTimeLine(),
                w.getLatitude(), w.getLongitude());
    }
}
