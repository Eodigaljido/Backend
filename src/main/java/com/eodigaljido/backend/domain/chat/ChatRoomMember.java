package com.eodigaljido.backend.domain.chat;

import com.eodigaljido.backend.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "chat_room_members",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_chat_room_members_room_user", columnNames = {"room_id", "user_id"})
    },
    indexes = {
        @Index(name = "idx_chat_room_members_room_id", columnList = "room_id"),
        @Index(name = "idx_chat_room_members_user_id", columnList = "user_id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ChatRoomMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private MemberRole role = MemberRole.MEMBER;

    @Column(name = "last_read_at")
    private LocalDateTime lastReadAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum MemberRole {
        ADMIN, MEMBER
    }
}
