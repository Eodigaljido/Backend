package com.eodigaljido.backend.dto.routehistory;

import com.eodigaljido.backend.dto.course.PageInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "루트 기록 피드 응답 (채팅 메시지 + 수정 이벤트 시간순 정렬)")
public record RouteHistoryFeedResponse(

        @Schema(description = "피드 항목 목록")
        List<RouteHistoryFeedItem> items,

        @Schema(description = "페이징 정보")
        PageInfo pageInfo
) {}
