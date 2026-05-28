package com.eodigaljido.backend.domain.schedule;

import com.eodigaljido.backend.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "course_schedule_participants",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_schedule_participant", columnNames = {"schedule_id", "user_id"})
    },
    indexes = {
        @Index(name = "idx_schedule_participants_schedule_id", columnList = "schedule_id"),
        @Index(name = "idx_schedule_participants_user_id", columnList = "user_id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CourseScheduleParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private CourseSchedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private ParticipantSource source = ParticipantSource.MANUAL;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum ParticipantSource {
        CHAT_ROOM, MANUAL
    }
}
