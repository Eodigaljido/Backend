package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.schedule.CourseSchedule;
import com.eodigaljido.backend.domain.user.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CourseScheduleRepository extends JpaRepository<CourseSchedule, Long> {

    @Query("""
            SELECT s FROM CourseSchedule s
            LEFT JOIN FETCH s.chatRoom cr
            LEFT JOIN FETCH s.course c
            LEFT JOIN FETCH s.owner o
            WHERE s.uuid = :uuid AND s.deletedAt IS NULL
            """)
    Optional<CourseSchedule> findByUuidAndDeletedAtIsNull(@Param("uuid") String uuid);

    @Query("""
            SELECT DISTINCT s FROM CourseSchedule s
            LEFT JOIN FETCH s.chatRoom cr
            LEFT JOIN FETCH s.course c
            LEFT JOIN FETCH s.owner o
            WHERE s.deletedAt IS NULL
              AND (:from IS NULL OR s.scheduledAt >= :from)
              AND (:to IS NULL OR s.scheduledAt <= :to)
              AND (:chatRoomUuid IS NULL OR cr.uuid = :chatRoomUuid)
              AND (:upcomingOnly = false OR s.scheduledAt >= :now)
              AND (
                    s.owner = :user
                    OR EXISTS (
                        SELECT m FROM ChatRoomMember m
                        WHERE m.room = s.chatRoom
                          AND m.user = :user
                          AND m.leftAt IS NULL
                    )
              )
            ORDER BY s.scheduledAt ASC, s.createdAt ASC
            """)
    List<CourseSchedule> findAccessibleSchedules(
            @Param("user") User user,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("chatRoomUuid") String chatRoomUuid,
            @Param("upcomingOnly") boolean upcomingOnly,
            @Param("now") LocalDateTime now
    );

    @Query("""
            SELECT DISTINCT s FROM CourseSchedule s
            LEFT JOIN FETCH s.chatRoom cr
            LEFT JOIN FETCH s.course c
            LEFT JOIN FETCH s.owner o
            WHERE s.deletedAt IS NULL
              AND s.scheduledAt >= :now
              AND (
                    s.owner = :user
                    OR EXISTS (
                        SELECT m FROM ChatRoomMember m
                        WHERE m.room = s.chatRoom
                          AND m.user = :user
                          AND m.leftAt IS NULL
                    )
              )
            ORDER BY s.scheduledAt ASC, s.createdAt ASC
            """)
    List<CourseSchedule> findNearestAccessibleSchedule(
            @Param("user") User user,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );
}
