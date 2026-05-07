package com.eodigaljido.backend.dto.course;

import com.eodigaljido.backend.domain.route.RouteWaypoint;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "코스 경유지(스텝) 정보")
public record CourseStepResponse(
        @Schema(description = "경유지 ID", example = "1")
        Long id,

        @Schema(description = "지점 이름", example = "경복궁")
        String name,

        @Schema(description = "체류 시간(분)", example = "60")
        Integer stayMinutes,

        @Schema(description = "위도", example = "37.5796212")
        BigDecimal latitude,

        @Schema(description = "경도", example = "126.9770162")
        BigDecimal longitude,

        @Schema(description = "주소", example = "서울특별시 종로구 사직로 161")
        String address
) {
    public static CourseStepResponse from(RouteWaypoint w) {
        return new CourseStepResponse(
                w.getId(),
                w.getName(),
                w.getStayMinutes(),
                w.getLatitude(),
                w.getLongitude(),
                w.getAddress()
        );
    }
}
