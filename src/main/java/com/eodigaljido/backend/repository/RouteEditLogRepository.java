package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.route.Route;
import com.eodigaljido.backend.domain.route.RouteEditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RouteEditLogRepository extends JpaRepository<RouteEditLog, Long> {

    @Query("SELECT el FROM RouteEditLog el JOIN FETCH el.editor " +
           "WHERE el.route = :route ORDER BY el.createdAt DESC")
    Page<RouteEditLog> findByRoute(@Param("route") Route route, Pageable pageable);

    @Query("SELECT el FROM RouteEditLog el JOIN FETCH el.editor " +
           "WHERE el.route = :route ORDER BY el.createdAt ASC")
    List<RouteEditLog> findByRouteOrderByCreatedAtAsc(@Param("route") Route route);
}
