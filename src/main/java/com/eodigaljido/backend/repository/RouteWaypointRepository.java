package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.route.Route;
import com.eodigaljido.backend.domain.route.RouteWaypoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteWaypointRepository extends JpaRepository<RouteWaypoint, Long> {

    List<RouteWaypoint> findByRouteOrderBySequenceAsc(Route route);

    void deleteAllByRoute(Route route);
}
