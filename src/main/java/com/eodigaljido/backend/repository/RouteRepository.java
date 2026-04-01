package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.route.Route;
import com.eodigaljido.backend.domain.route.Route.RouteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {

    Optional<Route> findByUuidAndStatusNot(String uuid, RouteStatus status);

    List<Route> findByUserIdAndStatusNot(Long userId, RouteStatus status);

    List<Route> findByUserIdAndIsSharedTrueAndStatusNot(Long userId, RouteStatus status);

    Optional<Route> findByShareTokenAndIsSharedTrueAndStatusNot(String shareToken, RouteStatus status);
}
