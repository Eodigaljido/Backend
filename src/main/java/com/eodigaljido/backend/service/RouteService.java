package com.eodigaljido.backend.service;

import com.eodigaljido.backend.domain.chat.ChatRoom;
import com.eodigaljido.backend.domain.notification.NotificationType;
import com.eodigaljido.backend.domain.route.Route;
import com.eodigaljido.backend.domain.route.Route.RouteStatus;
import com.eodigaljido.backend.domain.route.RouteWaypoint;
import com.eodigaljido.backend.domain.route.SavedRoute;
import com.eodigaljido.backend.domain.user.User;
import com.eodigaljido.backend.dto.route.*;
import com.eodigaljido.backend.event.NotificationEvent;
import com.eodigaljido.backend.exception.RouteException;
import com.eodigaljido.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final RouteRepository routeRepository;
    private final RouteWaypointRepository waypointRepository;
    private final SavedRouteRepository savedRouteRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final OnboardingAnswerRepository onboardingAnswerRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ──────────────────────────────────────────────────────────
    // CRUD
    // ──────────────────────────────────────────────────────────

    @Transactional
    public RouteResponse createRoute(Long userId, CreateRouteRequest req) {
        User user = findUser(userId);

        RouteStatus status = req.status() != null ? req.status() : RouteStatus.DRAFT;

        ChatRoom chatRoom = null;
        if (req.chatRoomUuid() != null) {
            chatRoom = chatRoomRepository.findByUuidAndDeletedAtIsNull(req.chatRoomUuid())
                    .orElseThrow(() -> new RouteException("채팅방을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        }

        Route route = Route.builder()
                .uuid(UUID.randomUUID().toString())
                .user(user)
                .title(req.title())
                .description(req.description())
                .status(status)
                .totalDistance(req.totalDistance())
                .estimatedTime(req.estimatedTime())
                .thumbnailUrl(req.thumbnailUrl())
                .chatRoom(chatRoom)
                .region(req.region())
                .activityType(req.activityType())
                .build();
        routeRepository.save(route);

        List<RouteWaypoint> waypoints = buildWaypoints(route, req.waypoints());
        waypointRepository.saveAll(waypoints);

        // 채팅방 연결 코스 생성 시 CHAT_ROUTE_CHANGED 알림
        if (chatRoom != null) {
            publishChatRouteChangedEvents(chatRoom, userId, route, "코스가 생성되었습니다: " + route.getTitle());
        }

        return RouteResponse.of(route, waypoints.stream().map(WaypointResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public RouteResponse getRoute(Long userId, Long id) {
        Route route = findActiveRouteById(id);
        if (!route.getUser().getId().equals(userId)) {
            throw new RouteException("해당 루트에 접근할 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
        return toRouteResponse(route);
    }

    @Transactional(readOnly = true)
    public List<RouteSummaryResponse> getMyRoutes(Long userId) {
        return routeRepository.findByUserIdAndStatusNot(userId, RouteStatus.DELETED)
                .stream()
                .map(RouteSummaryResponse::from)
                .toList();
    }

    @Transactional
    public RouteResponse updateRoute(Long userId, Long id, UpdateRouteRequest req) {
        Route route = findActiveRouteById(id);
        verifyOwner(route, userId);

        waypointRepository.deleteAllByRoute(route);

        route.update(req.title(), req.description(), req.totalDistance(),
                req.estimatedTime(), req.thumbnailUrl(), req.region(), req.activityType());

        List<RouteWaypoint> waypoints = buildWaypoints(route, req.waypoints());
        waypointRepository.saveAll(waypoints);

        // 채팅방 연결 코스 수정 시 CHAT_ROUTE_CHANGED 알림
        if (route.getChatRoom() != null) {
            publishChatRouteChangedEvents(route.getChatRoom(), userId, route,
                    "코스가 수정되었습니다: " + route.getTitle());
        }

        return RouteResponse.of(route, waypoints.stream().map(WaypointResponse::from).toList());
    }

    @Transactional
    public void deleteRoute(Long userId, Long id) {
        Route route = findActiveRouteById(id);
        verifyOwner(route, userId);
        route.markDeleted();
    }

    @Transactional
    public RouteResponse updateRouteStatus(Long userId, Long id, RouteStatus status) {
        if (status == RouteStatus.DELETED) {
            throw new RouteException("상태를 DELETED로 변경할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
        Route route = findActiveRouteById(id);
        verifyOwner(route, userId);
        route.updateStatus(status);
        return toRouteResponse(route);
    }

    // ──────────────────────────────────────────────────────────
    // 즐겨찾기(저장)
    // ──────────────────────────────────────────────────────────

    @Transactional
    public void saveRoute(Long userId, Long id) {
        Route route = findActiveRouteById(id);

        // 본인 루트이거나 공유된 루트만 즐겨찾기 가능
        if (!route.getUser().getId().equals(userId) && !route.isShared()) {
            throw new RouteException("공유되지 않은 루트는 저장할 수 없습니다.", HttpStatus.FORBIDDEN);
        }

        if (savedRouteRepository.existsByUserIdAndRouteId(userId, route.getId())) {
            throw new RouteException("이미 저장된 루트입니다.", HttpStatus.CONFLICT);
        }
        User user = findUser(userId);
        savedRouteRepository.save(SavedRoute.builder()
                .user(user)
                .route(route)
                .build());

        // 코스 소유자에게 ROUTE_FAVORITED 알림 (자기 코스 제외)
        if (!route.getUser().getId().equals(userId)) {
            String saverNickname = user.getUserId();
            eventPublisher.publishEvent(NotificationEvent.of(
                    route.getUser().getId(), userId,
                    NotificationType.ROUTE_FAVORITED,
                    "코스 즐겨찾기",
                    saverNickname + "님이 회원님의 코스를 즐겨찾기했습니다: " + route.getTitle(),
                    route.getUuid(), "ROUTE"
            ));
        }
    }

    @Transactional
    public void unsaveRoute(Long userId, Long id) {
        Route route = findActiveRouteById(id);
        SavedRoute saved = savedRouteRepository.findByUserIdAndRouteId(userId, route.getId())
                .orElseThrow(() -> new RouteException("저장된 루트가 아닙니다.", HttpStatus.NOT_FOUND));
        savedRouteRepository.delete(saved);
    }

    @Transactional(readOnly = true)
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
    public void enableSharing(Long userId, Long id) {
        Route route = findActiveRouteById(id);
        verifyOwner(route, userId);
        route.enableSharing();

        // 취향 매칭 사용자들에게 ROUTE_RECOMMENDED 알림 (비동기로 처리됨)
        if (route.getRegion() != null || route.getActivityType() != null) {
            onboardingAnswerRepository
                    .findMatchingUsers(route.getRegion(), route.getActivityType())
                    .stream()
                    .filter(answer -> !answer.getUser().getId().equals(userId))
                    .limit(1000)
                    .forEach(answer -> eventPublisher.publishEvent(NotificationEvent.of(
                            answer.getUser().getId(), userId,
                            NotificationType.ROUTE_RECOMMENDED,
                            "추천 코스",
                            "취향에 맞는 코스가 공유되었습니다: " + route.getTitle(),
                            route.getUuid(), "ROUTE"
                    )));
        }
    }

    @Transactional
    public void disableSharing(Long userId, Long id) {
        Route route = findActiveRouteById(id);
        verifyOwner(route, userId);
        route.disableSharing();
    }

    @Transactional(readOnly = true)
    public List<SharedRouteSummaryResponse> getPublicSharedRoutes() {
        List<Route> routes = routeRepository.findSharedRoutesWithUser(RouteStatus.DELETED);
        if (routes.isEmpty()) {
            return List.of();
        }

        // 배치로 첫 번째 경유지를 한 번에 조회 (N+1 방지)
        Map<Long, String> firstWaypointNames = waypointRepository.findFirstWaypointsByRoutes(routes)
                .stream()
                .collect(Collectors.toMap(
                        w -> w.getRoute().getId(),
                        RouteWaypoint::getName,
                        (a, b) -> a
                ));

        return routes.stream()
                .map(route -> new SharedRouteSummaryResponse(
                        route.getUuid(),
                        route.getTitle(),
                        route.getDescription(),
                        route.getStatus(),
                        route.getTotalDistance(),
                        route.getEstimatedTime(),
                        route.getThumbnailUrl(),
                        firstWaypointNames.get(route.getId()),
                        route.getUser().getUuid(),
                        route.getUser().getUserId()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public RouteResponse getPublicSharedRoute(String uuid) {
        Route route = routeRepository.findByUuidAndStatusNot(uuid, RouteStatus.DELETED)
                .filter(Route::isShared)
                .orElseThrow(() -> new RouteException("공유된 루트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        return toRouteResponse(route);
    }

    @Transactional(readOnly = true)
    public List<RouteSummaryResponse> getSharingRoutes(Long userId) {
        return routeRepository.findByUserIdAndIsSharedTrueAndStatusNot(userId, RouteStatus.DELETED)
                .stream()
                .map(RouteSummaryResponse::from)
                .toList();
    }

    // ──────────────────────────────────────────────────────────
    // 코스 복사
    // ──────────────────────────────────────────────────────────

    @Transactional
    public RouteResponse copyRoute(Long userId, Long id) {
        Route original = findActiveRouteById(id);
        if (!original.isShared() && !original.getUser().getId().equals(userId)) {
            throw new RouteException("공유되지 않은 루트는 복사할 수 없습니다.", HttpStatus.FORBIDDEN);
        }

        User user = findUser(userId);

        Route copied = Route.builder()
                .uuid(UUID.randomUUID().toString())
                .user(user)
                .title(original.getTitle() + " (복사)")
                .description(original.getDescription())
                .status(RouteStatus.DRAFT)
                .totalDistance(original.getTotalDistance())
                .estimatedTime(original.getEstimatedTime())
                .thumbnailUrl(original.getThumbnailUrl())
                .region(original.getRegion())
                .activityType(original.getActivityType())
                .build();
        routeRepository.save(copied);

        List<RouteWaypoint> waypoints = waypointRepository.findByRouteOrderBySequenceAsc(original)
                .stream()
                .map(w -> RouteWaypoint.builder()
                        .route(copied)
                        .sequence(w.getSequence())
                        .name(w.getName())
                        .latitude(w.getLatitude())
                        .longitude(w.getLongitude())
                        .address(w.getAddress())
                        .memo(w.getMemo())
                        .build())
                .toList();
        waypointRepository.saveAll(waypoints);

        // 원본 소유자에게 ROUTE_USED 알림 (자기 코스 제외)
        if (!original.getUser().getId().equals(userId)) {
            eventPublisher.publishEvent(NotificationEvent.of(
                    original.getUser().getId(), userId,
                    NotificationType.ROUTE_USED,
                    "코스 사용",
                    user.getUserId() + "님이 회원님의 코스를 사용했습니다: " + original.getTitle(),
                    original.getUuid(), "ROUTE"
            ));
        }

        return RouteResponse.of(copied, waypoints.stream().map(WaypointResponse::from).toList());
    }

    // ──────────────────────────────────────────────────────────
    // private helpers
    // ──────────────────────────────────────────────────────────

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RouteException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    private Route findActiveRouteById(Long id) {
        return routeRepository.findByIdAndStatusNot(id, RouteStatus.DELETED)
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

    private void publishChatRouteChangedEvents(ChatRoom chatRoom, Long actorUserId,
                                               Route route, String body) {
        chatRoomMemberRepository.findByRoomAndLeftAtIsNull(chatRoom)
                .stream()
                .filter(m -> !m.getUser().getId().equals(actorUserId))
                .forEach(m -> eventPublisher.publishEvent(NotificationEvent.of(
                        m.getUser().getId(), actorUserId,
                        NotificationType.CHAT_ROUTE_CHANGED,
                        "코스 변경",
                        body,
                        route.getUuid(), "ROUTE"
                )));
    }
}
