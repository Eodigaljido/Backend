package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.route.Route;
import com.eodigaljido.backend.domain.route.RouteLeg;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteLegRepository extends JpaRepository<RouteLeg, Long> {

    List<RouteLeg> findByRouteOrderBySequenceAsc(Route route);

    void deleteAllByRoute(Route route);
}
