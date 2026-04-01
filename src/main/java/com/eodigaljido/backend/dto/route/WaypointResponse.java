package com.eodigaljido.backend.dto.route;

import com.eodigaljido.backend.domain.route.RouteWaypoint;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "경유지 정보")
public record WaypointResponse(
        @Schema(description = "경유지 순서 (1부터 시작)", example = "1")
        int sequence,

        @Schema(description = "지점 이름", example = "경복궁")
        String name,

        @Schema(description = "위도", example = "37.5796212")
        BigDecimal latitude,

        @Schema(description = "경도", example = "126.9770162")
        BigDecimal longitude,

        @Schema(description = "주소", example = "서울특별시 종로구 사직로 161")
        String address,

        @Schema(description = "메모", example = "정문 쪽에서 진입")
        String memo
) {
    public static WaypointResponse from(RouteWaypoint w) {
        return new WaypointResponse(w.getSequence(), w.getName(),
                w.getLatitude(), w.getLongitude(), w.getAddress(), w.getMemo());
    }
}
