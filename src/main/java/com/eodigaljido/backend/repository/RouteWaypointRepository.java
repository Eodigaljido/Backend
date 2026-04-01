package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.route.Route;
import com.eodigaljido.backend.domain.route.RouteWaypoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RouteWaypointRepository extends JpaRepository<RouteWaypoint, Long> {

    List<RouteWaypoint> findByRouteOrderBySequenceAsc(Route route);

    Optional<RouteWaypoint> findTopByRouteOrderBySequenceAsc(Route route);

    void deleteAllByRoute(Route route);
}
