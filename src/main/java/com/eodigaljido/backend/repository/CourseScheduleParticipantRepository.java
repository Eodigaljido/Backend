package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.schedule.CourseSchedule;
import com.eodigaljido.backend.domain.schedule.CourseScheduleParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseScheduleParticipantRepository extends JpaRepository<CourseScheduleParticipant, Long> {

    @Query("SELECT p FROM CourseScheduleParticipant p JOIN FETCH p.user WHERE p.schedule = :schedule")
    List<CourseScheduleParticipant> findByScheduleWithUser(@Param("schedule") CourseSchedule schedule);

    @Query("SELECT p FROM CourseScheduleParticipant p JOIN FETCH p.user WHERE p.schedule IN :schedules")
    List<CourseScheduleParticipant> findByScheduleInWithUser(@Param("schedules") List<CourseSchedule> schedules);

    @Modifying
    @Query("DELETE FROM CourseScheduleParticipant p WHERE p.schedule = :schedule")
    void deleteBySchedule(@Param("schedule") CourseSchedule schedule);

    long countBySchedule(CourseSchedule schedule);
}
