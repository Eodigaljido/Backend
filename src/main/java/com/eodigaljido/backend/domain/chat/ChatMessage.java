package com.eodigaljido.backend.domain.chat;

import com.eodigaljido.backend.domain.route.Route;
import com.eodigaljido.backend.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "chat_messages",
    indexes = {
        @Index(name = "idx_chat_messages_uuid", columnList = "uuid"),
        @Index(name = "idx_chat_messages_room_created", columnList = "room_id, created_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 36, unique = true, nullable = false)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MessageType type = MessageType.TEXT;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "attachment_url", length = 512)
    private String attachmentUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ref_route_id")
    private Route refRoute;

    // 이 메시지가 속한 루트의 공동편집 기록 조회용 FK. ROUTE 타입 채팅방에서 오간 메시지/편집 이벤트에만 채워짐
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private Route route;

    // COURSE 계열 type(STOP_ADDED 등)의 구체적 변경 내용 (JSON 문자열)
    @Column(name = "edit_details", columnDefinition = "TEXT")
    private String editDetails;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    public void edit(String newContent) {
        this.content = newContent;
        this.editedAt = LocalDateTime.now();
    }

    public enum MessageType {
        TEXT, IMAGE, ROUTE, SYSTEM,
        ROUTE_UPDATED, STOP_ADDED, STOP_REMOVED, STOP_MODIFIED, LEG_UPDATED, TITLE_CHANGED, EDITING_COMPLETED, EDITING_RESUMED;

        public boolean isCourseEvent() {
            return this == ROUTE_UPDATED || this == STOP_ADDED || this == STOP_REMOVED || this == STOP_MODIFIED
                    || this == LEG_UPDATED || this == TITLE_CHANGED
                    || this == EDITING_COMPLETED || this == EDITING_RESUMED;
        }

        public String describe(String nickname) {
            String suffix = switch (this) {
                case STOP_ADDED -> "님이 경유지를 추가했습니다";
                case STOP_REMOVED -> "님이 경유지를 삭제했습니다";
                case STOP_MODIFIED -> "님이 경유지를 수정했습니다";
                case LEG_UPDATED -> "님이 이동 구간을 수정했습니다";
                case TITLE_CHANGED -> "님이 루트 이름을 변경했습니다";
                case EDITING_COMPLETED -> "님이 편집을 완료했습니다";
                case EDITING_RESUMED -> "님이 편집을 재개했습니다";
                case ROUTE_UPDATED -> "님이 루트를 수정했습니다";
                default -> "님이 메시지를 보냈습니다";
            };
            return nickname + suffix;
        }
    }
}
