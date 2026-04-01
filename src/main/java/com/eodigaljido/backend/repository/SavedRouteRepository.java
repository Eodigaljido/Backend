package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.route.SavedRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SavedRouteRepository extends JpaRepository<SavedRoute, Long> {

    boolean existsByUserIdAndRouteId(Long userId, Long routeId);

    Optional<SavedRoute> findByUserIdAndRouteId(Long userId, Long routeId);

    List<SavedRoute> findByUserIdOrderByCreatedAtDesc(Long userId);
}
