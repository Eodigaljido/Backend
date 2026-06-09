package com.eodigaljido.backend.controller;

import com.eodigaljido.backend.dto.common.ErrorResponse;
import com.eodigaljido.backend.service.NotificationSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/notification-settings")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "알림 및 알림 설정 API")
public class NotificationSettingController {

    private final NotificationSettingService notificationSettingService;

    @GetMapping
    @Operation(
            summary = "알림 설정 조회",
            description = """
                    로그인 사용자의 전체 알림 설정을 `key: boolean` 형태로 반환합니다.

                    저장된 값이 없는 항목과 신규 사용자의 설정은 기본값 `true`로 반환합니다.

                    **채팅**
                    - `chatMessage`: 새 채팅 메시지
                    - `chatMention`: 멘션 알림
                    - `chatInvite`: 채팅방 초대
                    - `chatCourseShared`: 채팅방 코스 공유
                    - `chatCourseEdited`: 채팅방 연결 코스 생성·수정

                    **친구**
                    - `friendRequest`: 친구 요청
                    - `friendAccepted`: 친구 요청 수락

                    **모임**
                    - `meetJoinRequest`: 모임 가입 신청
                    - `meetJoinResult`: 모임 가입 신청 승인·거절 결과
                    - `meetNewMember`: 새 모임 멤버 참여
                    - `meetNewPost`: 새 모임 게시물
                    - `meetNewRoute`: 새 모임 루트 공유
                    - `meetNewChatRoom`: 새 모임 채팅방

                    **코스**
                    - `courseRecommended`: 맞춤 코스 추천
                    - `courseFavoritedOrUsed`: 내 코스 즐겨찾기·사용
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "전체 알림 설정 반환",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE),
                            examples = @ExampleObject(name = "전체 알림 설정 예시", value = """
                                    {
                                      "chatMessage": true,
                                      "chatMention": true,
                                      "chatInvite": false,
                                      "chatCourseShared": true,
                                      "chatCourseEdited": true,
                                      "friendRequest": true,
                                      "friendAccepted": true,
                                      "meetJoinRequest": true,
                                      "meetJoinResult": true,
                                      "meetNewMember": false,
                                      "meetNewPost": true,
                                      "meetNewRoute": true,
                                      "meetNewChatRoom": true,
                                      "courseRecommended": true,
                                      "courseFavoritedOrUsed": true
                                    }
                                    """)
                    )),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Map<String, Boolean>> getSettings(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(notificationSettingService.getSettings(Long.parseLong(userDetails.getUsername())));
    }

    @PatchMapping
    @Operation(
            summary = "알림 설정 부분 변경",
            description = """
                    변경할 알림 설정만 `key: boolean` 형태로 전달합니다.

                    - 단일 토글: 변경할 key 하나만 전달
                    - 여러 항목 변경: 변경할 key들을 함께 전달
                    - 전체 알림 ON/OFF: 15개 key 전체를 동일한 값으로 전달

                    요청에 포함되지 않은 key의 기존 설정은 변경되지 않습니다.
                    변경 후에는 기본값이 채워진 전체 알림 설정을 반환합니다.
                    """,
            security = @SecurityRequirement(name = "Bearer")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "변경 반영된 전체 알림 설정 반환",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE),
                            examples = @ExampleObject(name = "변경 후 전체 설정 예시", value = """
                                    {
                                      "chatMessage": false,
                                      "chatMention": true,
                                      "chatInvite": true,
                                      "chatCourseShared": true,
                                      "chatCourseEdited": true,
                                      "friendRequest": true,
                                      "friendAccepted": true,
                                      "meetJoinRequest": true,
                                      "meetJoinResult": true,
                                      "meetNewMember": true,
                                      "meetNewPost": true,
                                      "meetNewRoute": true,
                                      "meetNewChatRoom": true,
                                      "courseRecommended": true,
                                      "courseFavoritedOrUsed": true
                                    }
                                    """)
                    )),
            @ApiResponse(responseCode = "400", description = "지원하지 않는 key 또는 잘못된 값",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 토큰이 없거나 만료됨",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Map<String, Boolean>> updateSettings(
            @AuthenticationPrincipal UserDetails userDetails,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "변경할 알림 설정 일부 또는 전체",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(type = "object", additionalProperties = Schema.AdditionalPropertiesValue.TRUE),
                            examples = {
                                    @ExampleObject(name = "단일 설정 변경", value = """
                                            {
                                              "chatMessage": false
                                            }
                                            """),
                                    @ExampleObject(name = "여러 설정 변경", value = """
                                            {
                                              "chatMessage": false,
                                              "chatMention": false,
                                              "meetNewPost": true
                                            }
                                            """)
                            }
                    )
            )
            @RequestBody Map<String, Boolean> updates) {
        return ResponseEntity.ok(notificationSettingService.updateSettings(
                Long.parseLong(userDetails.getUsername()), updates));
    }
}
