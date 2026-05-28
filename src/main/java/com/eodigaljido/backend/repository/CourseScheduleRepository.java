package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.schedule.CourseSchedule;
import com.eodigaljido.backend.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface CourseScheduleRepository extends JpaRepository<CourseSchedule, Long> {

    @Query("SELECT s FROM CourseSchedule s LEFT JOIN FETCH s.chatRoom WHERE s.uuid = :uuid AND s.deletedAt IS NULL")
    Optional<CourseSchedule> findByUuidAndDeletedAtIsNull(@Param("uuid") String uuid);

    @Query("""
        SELECT s FROM CourseSchedule s LEFT JOIN FETCH s.chatRoom
        WHERE s.owner = :owner
          AND s.deletedAt IS NULL
          AND (:from IS NULL OR s.scheduledAt >= :from)
          AND (:to IS NULL OR s.scheduledAt < :to)
        ORDER BY s.scheduledAt ASC
    """)
    Page<CourseSchedule> findByOwnerAndDateRange(
        @Param("owner") User owner,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        Pageable pageable
    );

    @Query("""
        SELECT s FROM CourseSchedule s LEFT JOIN FETCH s.chatRoom
        WHERE s.owner = :owner
          AND s.scheduledAt >= :now
          AND s.deletedAt IS NULL
        ORDER BY s.scheduledAt ASC
    """)
    Page<CourseSchedule> findNearestByOwner(
        @Param("owner") User owner,
        @Param("now") LocalDateTime now,
        Pageable pageable
    );
}
