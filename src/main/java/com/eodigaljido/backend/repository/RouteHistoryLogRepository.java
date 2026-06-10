package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.route.Route;
import com.eodigaljido.backend.domain.route.RouteHistoryLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RouteHistoryLogRepository extends JpaRepository<RouteHistoryLog, Long> {

    @Query("SELECT l FROM RouteHistoryLog l JOIN FETCH l.actor " +
           "WHERE l.route = :route ORDER BY l.createdAt ASC")
    Page<RouteHistoryLog> findByRouteOrderByCreatedAtAsc(@Param("route") Route route, Pageable pageable);
}
