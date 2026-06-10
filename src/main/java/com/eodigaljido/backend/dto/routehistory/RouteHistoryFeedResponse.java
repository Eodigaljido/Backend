package com.eodigaljido.backend.dto.routehistory;

import com.eodigaljido.backend.dto.course.PageInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "루트 기록 상세 피드 응답 (채팅 메시지 + 루트 수정 이벤트를 발생 시각 오름차순으로 정렬)")
public record RouteHistoryFeedResponse(

        @Schema(description = "피드 항목 목록")
        List<RouteHistoryFeedItem> items,

        @Schema(description = "페이징 정보")
        PageInfo pageInfo
) {}
