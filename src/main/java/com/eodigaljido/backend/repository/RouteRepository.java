package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.route.Route;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteRepository extends JpaRepository<Route, Long> {

    List<Route> findByStatusAndIsSharedAndDeletedAtIsNull(
            Route.RouteStatus status, boolean isShared, Pageable pageable);
}
