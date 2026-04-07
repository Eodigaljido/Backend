package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.route.Route;
import com.eodigaljido.backend.domain.route.Route.RouteStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {

    Optional<Route> findByIdAndStatusNot(Long id, RouteStatus status);

    Optional<Route> findByUuidAndStatusNot(String uuid, RouteStatus status);

    List<Route> findByUserIdAndStatusNot(Long userId, RouteStatus status);

    List<Route> findByUserIdAndIsSharedTrueAndStatusNot(Long userId, RouteStatus status);

    List<Route> findByIsSharedTrueAndStatusNot(RouteStatus status);

    List<Route> findByStatusAndIsSharedAndDeletedAtIsNull(RouteStatus status, boolean isShared, Pageable pageable);

    @Query("SELECT r FROM Route r JOIN FETCH r.user WHERE r.isShared = true AND r.status <> :status")
    List<Route> findSharedRoutesWithUser(@Param("status") RouteStatus status);
}
