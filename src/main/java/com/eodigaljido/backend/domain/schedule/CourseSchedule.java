package com.eodigaljido.backend.domain.schedule;

import com.eodigaljido.backend.domain.chat.ChatRoom;
import com.eodigaljido.backend.domain.common.BaseTimeEntity;
import com.eodigaljido.backend.domain.route.Route;
import com.eodigaljido.backend.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "course_schedules",
    indexes = {
        @Index(name = "idx_course_schedules_uuid", columnList = "uuid"),
        @Index(name = "idx_course_schedules_owner_scheduled_at", columnList = "owner_id, scheduled_at"),
        @Index(name = "idx_course_schedules_chat_room_scheduled_at", columnList = "chat_room_id, scheduled_at"),
        @Index(name = "idx_course_schedules_scheduled_at", columnList = "scheduled_at"),
        @Index(name = "idx_course_schedules_deleted_at", columnList = "deleted_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class CourseSchedule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 36, unique = true, nullable = false)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(length = 100, nullable = false)
    private String title;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Route course;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public void updateChatRoom(ChatRoom chatRoom) {
        this.chatRoom = chatRoom;
    }

    public void updateCourse(Route course) {
        this.course = course;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
