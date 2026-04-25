package com.eodigaljido.backend.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "채팅 메시지 응답")
public record ChatMessageResponse(
        @Schema(description = "메시지 UUID", example = "550e8400-e29b-41d4-a716-446655440001")
        String uuid,

        @Schema(description = "발신자 UUID", example = "550e8400-e29b-41d4-a716-446655440002")
        String senderUuid,

        @Schema(description = "발신자 닉네임", example = "홍길동")
        String senderNickname,

        @Schema(description = "발신자 프로필 이미지 URL (없으면 null)", example = "https://example.com/profile.jpg")
        String senderProfileImageUrl,

        @Schema(description = "메시지 타입 (TEXT, IMAGE, ROUTE)", example = "TEXT")
        String messageType,

        @Schema(description = "메시지 내용 (삭제된 경우 null, IMAGE 타입은 null)", example = "안녕하세요!")
        String content,

        @Schema(description = "첨부 이미지 URL (messageType=IMAGE 일 때만, 없으면 null)", example = "/uploads/chat-messages/room-uuid/msg-uuid.jpg")
        String attachmentUrl,

        @Schema(description = "공유된 루트 UUID (messageType=ROUTE 일 때만, 없으면 null)", example = "550e8400-e29b-41d4-a716-446655440000")
        String routeUuid,

        @Schema(description = "공유된 루트 제목 (messageType=ROUTE 일 때만, 없으면 null)", example = "북한산 둘레길")
        String routeTitle,

        @Schema(description = "공유된 루트 썸네일 URL (messageType=ROUTE 일 때만, 없으면 null)", example = "https://example.com/thumbnail.jpg")
        String routeThumbnailUrl,

        @Schema(description = "메시지 전송 시각", example = "2026-04-01T10:00:00")
        LocalDateTime createdAt,

        @Schema(description = "메시지 수정 시각 (수정된 적 없으면 null)", example = "2026-04-01T10:05:00")
        LocalDateTime editedAt,

        @Schema(description = "메시지 삭제 여부", example = "false")
        boolean isDeleted
) {}
