package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.route.Route;
import com.eodigaljido.backend.domain.route.RouteWaypoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RouteWaypointRepository extends JpaRepository<RouteWaypoint, Long> {

    List<RouteWaypoint> findByRouteOrderBySequenceAsc(Route route);

    Optional<RouteWaypoint> findTopByRouteOrderBySequenceAsc(Route route);

    void deleteAllByRoute(Route route);

    @Query("SELECT w FROM RouteWaypoint w WHERE w.route IN :routes AND w.sequence = " +
           "(SELECT MIN(w2.sequence) FROM RouteWaypoint w2 WHERE w2.route = w.route)")
    List<RouteWaypoint> findFirstWaypointsByRoutes(@Param("routes") List<Route> routes);
}
