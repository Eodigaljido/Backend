package com.eodigaljido.backend.domain.chat;

import com.eodigaljido.backend.domain.common.BaseTimeEntity;
import com.eodigaljido.backend.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "chat_rooms",
    indexes = {
        @Index(name = "idx_chat_rooms_uuid", columnList = "uuid"),
        @Index(name = "idx_chat_rooms_created_by", columnList = "created_by"),
        @Index(name = "idx_chat_rooms_type", columnList = "type")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class ChatRoom extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 36, unique = true, nullable = false)
    private String uuid;

    @Column(length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private RoomType type = RoomType.DIRECT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void updateName(String name) {
        this.name = name;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public enum RoomType {
        DIRECT, GROUP
    }
}
