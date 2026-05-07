package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.route.Route;
import com.eodigaljido.backend.domain.route.RouteReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface RouteReviewRepository extends JpaRepository<RouteReview, Long> {

    List<RouteReview> findByRouteOrderByCreatedAtDesc(Route route);

    long countByRoute(Route route);

    @Query("SELECT AVG(r.rating) FROM RouteReview r WHERE r.route = :route")
    BigDecimal findAverageRatingByRoute(@Param("route") Route route);
}
