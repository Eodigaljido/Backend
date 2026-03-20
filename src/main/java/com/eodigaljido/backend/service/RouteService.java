package com.eodigaljido.backend.service;

import com.eodigaljido.backend.domain.route.Route;
import com.eodigaljido.backend.domain.route.Route.RouteStatus;
import com.eodigaljido.backend.domain.route.RouteWaypoint;
import com.eodigaljido.backend.domain.route.SavedRoute;
import com.eodigaljido.backend.domain.user.User;
import com.eodigaljido.backend.dto.route.*;
import com.eodigaljido.backend.exception.RouteException;
import com.eodigaljido.backend.repository.RouteRepository;
import com.eodigaljido.backend.repository.RouteWaypointRepository;
import com.eodigaljido.backend.repository.SavedRouteRepository;
import com.eodigaljido.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final RouteRepository routeRepository;
    private final RouteWaypointRepository waypointRepository;
    private final SavedRouteRepository savedRouteRepository;
    private final UserRepository userRepository;

    // ──────────────────────────────────────────────────────────
    // CRUD
    // ──────────────────────────────────────────────────────────

    @Transactional
    public RouteResponse createRoute(Long userId, CreateRouteRequest req) {
        User user = findUser(userId);

        RouteStatus status = req.status() != null ? req.status() : RouteStatus.DRAFT;

        Route route = Route.builder()
                .uuid(UUID.randomUUID().toString())
                .user(user)
                .title(req.title())
                .description(req.description())
                .status(status)
                .totalDistance(req.totalDistance())
                .estimatedTime(req.estimatedTime())
                .thumbnailUrl(req.thumbnailUrl())
                .build();
        routeRepository.save(route);

        List<RouteWaypoint> waypoints = buildWaypoints(route, req.waypoints());
        waypointRepository.saveAll(waypoints);

        return RouteResponse.of(route, waypoints.stream().map(WaypointResponse::from).toList());
    }

    public RouteResponse getRoute(Long userId, String uuid) {
        Route route = findActiveRoute(uuid);
        if (!route.getUser().getId().equals(userId)) {
            throw new RouteException("해당 루트에 접근할 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
        return toRouteResponse(route);
    }

    public List<RouteSummaryResponse> getMyRoutes(Long userId) {
        return routeRepository.findByUserIdAndStatusNot(userId, RouteStatus.DELETED)
                .stream()
                .map(RouteSummaryResponse::from)
                .toList();
    }

    @Transactional
    public RouteResponse updateRoute(Long userId, String uuid, UpdateRouteRequest req) {
        Route route = findActiveRoute(uuid);
        verifyOwner(route, userId);

        waypointRepository.deleteAllByRoute(route);

        route.update(req.title(), req.description(), req.totalDistance(),
                req.estimatedTime(), req.thumbnailUrl());

        List<RouteWaypoint> waypoints = buildWaypoints(route, req.waypoints());
        waypointRepository.saveAll(waypoints);

        return RouteResponse.of(route, waypoints.stream().map(WaypointResponse::from).toList());
    }

    @Transactional
    public void deleteRoute(Long userId, String uuid) {
        Route route = findActiveRoute(uuid);
        verifyOwner(route, userId);
        route.markDeleted();
    }

    @Transactional
    public RouteResponse updateRouteStatus(Long userId, String uuid, RouteStatus status) {
        if (status == RouteStatus.DELETED) {
            throw new RouteException("상태를 DELETED로 변경할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
        Route route = findActiveRoute(uuid);
        verifyOwner(route, userId);
        route.updateStatus(status);
        return toRouteResponse(route);
    }

    // ──────────────────────────────────────────────────────────
    // 즐겨찾기(저장)
    // ──────────────────────────────────────────────────────────

    @Transactional
    public void saveRoute(Long userId, String uuid) {
        Route route = findActiveRoute(uuid);
        if (savedRouteRepository.existsByUserIdAndRouteId(userId, route.getId())) {
            throw new RouteException("이미 저장된 루트입니다.", HttpStatus.CONFLICT);
        }
        User user = findUser(userId);
        savedRouteRepository.save(SavedRoute.builder()
                .user(user)
                .route(route)
                .build());
    }

    @Transactional
    public void unsaveRoute(Long userId, String uuid) {
        Route route = findActiveRoute(uuid);
        SavedRoute saved = savedRouteRepository.findByUserIdAndRouteId(userId, route.getId())
                .orElseThrow(() -> new RouteException("저장된 루트가 아닙니다.", HttpStatus.NOT_FOUND));
        savedRouteRepository.delete(saved);
    }

    public List<RouteSummaryResponse> getSavedRoutes(Long userId) {
        return savedRouteRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(SavedRoute::getRoute)
                .filter(route -> route.getStatus() != RouteStatus.DELETED)
                .map(RouteSummaryResponse::from)
                .toList();
    }

    // ──────────────────────────────────────────────────────────
    // 공유
    // ──────────────────────────────────────────────────────────

    @Transactional
    public String enableSharing(Long userId, String uuid) {
        Route route = findActiveRoute(uuid);
        verifyOwner(route, userId);
        String shareToken = UUID.randomUUID().toString();
        route.enableSharing(shareToken);
        return shareToken;
    }

    @Transactional
    public void disableSharing(Long userId, String uuid) {
        Route route = findActiveRoute(uuid);
        verifyOwner(route, userId);
        route.disableSharing();
    }

    public RouteResponse getSharedRoute(String shareToken) {
        Route route = routeRepository.findByShareTokenAndIsSharedTrueAndStatusNot(shareToken, RouteStatus.DELETED)
                .orElseThrow(() -> new RouteException("공유된 루트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        return toRouteResponse(route);
    }

    public List<RouteSummaryResponse> getSharingRoutes(Long userId) {
        return routeRepository.findByUserIdAndIsSharedTrue(userId)
                .stream()
                .map(RouteSummaryResponse::from)
                .toList();
    }

    // ──────────────────────────────────────────────────────────
    // private helpers
    // ──────────────────────────────────────────────────────────

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RouteException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    private Route findActiveRoute(String uuid) {
        return routeRepository.findByUuidAndStatusNot(uuid, RouteStatus.DELETED)
                .orElseThrow(() -> new RouteException("루트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    private void verifyOwner(Route route, Long userId) {
        if (!route.getUser().getId().equals(userId)) {
            throw new RouteException("해당 루트에 접근할 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
    }

    private List<RouteWaypoint> buildWaypoints(Route route, List<WaypointRequest> requests) {
        if (requests == null || requests.isEmpty()) return List.of();
        return requests.stream()
                .map(w -> RouteWaypoint.builder()
                        .route(route)
                        .sequence(w.sequence())
                        .name(w.name())
                        .latitude(w.latitude())
                        .longitude(w.longitude())
                        .address(w.address())
                        .memo(w.memo())
                        .build())
                .toList();
    }

    private RouteResponse toRouteResponse(Route route) {
        List<WaypointResponse> waypoints = waypointRepository.findByRouteOrderBySequenceAsc(route)
                .stream()
                .map(WaypointResponse::from)
                .toList();
        return RouteResponse.of(route, waypoints);
    }
}
