package com.eodigaljido.backend.service;

import com.eodigaljido.backend.domain.route.Route;
import com.eodigaljido.backend.domain.route.Route.RouteStatus;
import com.eodigaljido.backend.domain.route.RouteLeg;
import com.eodigaljido.backend.domain.route.RouteReview;
import com.eodigaljido.backend.domain.route.RouteWaypoint;
import com.eodigaljido.backend.domain.route.SavedRoute;
import com.eodigaljido.backend.domain.following.FollowingNewsActionType;
import com.eodigaljido.backend.domain.user.User;
import com.eodigaljido.backend.dto.course.*;
import com.eodigaljido.backend.repository.RouteLegRepository;
import com.eodigaljido.backend.exception.RouteException;
import com.eodigaljido.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final RouteRepository routeRepository;
    private final RouteWaypointRepository waypointRepository;
    private final RouteLegRepository legRepository;
    private final RouteReviewRepository reviewRepository;
    private final SavedRouteRepository savedRouteRepository;
    private final UserRepository userRepository;
    private final FollowingNewsService followingNewsService;

    // ──────────────────────────────────────────────────────────
    // 홈 코스 목록 (인기/최근 공유 코스)
    // ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CourseItemResponse> getHomeCourses(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "views"));
        List<Route> routes = routeRepository.findSharedCoursesForHome(RouteStatus.DELETED, pageable);
        return routes.stream().map(this::toCourseItem).toList();
    }

    // ──────────────────────────────────────────────────────────
    // 공유 코스 목록 (필터/페이징)
    // ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CoursePageResponse getPublicCourses(String tab, String category, String region,
                                               String sort, String q, int page, int size,
                                               Long currentUserId) {
        Sort sortSpec = resolveSort(tab, sort);
        Pageable pageable = PageRequest.of(page, size, sortSpec);

        Page<Route> result;

        if ("friends".equals(tab) && currentUserId != null) {
            result = routeRepository.findSharedCoursesByFriends(
                    currentUserId, RouteStatus.DELETED, category, region, q, pageable);
        } else {
            result = routeRepository.findSharedCourses(
                    RouteStatus.DELETED, category, region, q, pageable);
        }

        List<CourseItemResponse> items = result.getContent().stream()
                .map(this::toCourseItem)
                .toList();

        PageInfo pageInfo = new PageInfo(
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );

        return new CoursePageResponse(items, pageInfo);
    }

    // ──────────────────────────────────────────────────────────
    // 공유 코스 상세 (조회수 증가 포함)
    // ──────────────────────────────────────────────────────────

    @Transactional
    public CourseDetailResponse getCourseDetail(String courseId) {
        Route route = findSharedRoute(courseId);
        route.incrementViews();

        List<CourseStepResponse> steps = loadSteps(route);
        List<ReviewResponse> reviews = reviewRepository.findByRouteOrderByCreatedAtDesc(route)
                .stream().map(ReviewResponse::from).toList();

        return CourseDetailResponse.of(route, steps, reviews);
    }

    // ──────────────────────────────────────────────────────────
    // 리뷰 작성
    // ──────────────────────────────────────────────────────────

    @Transactional
    public ReviewResponse writeReview(String courseId, WriteReviewRequest req, Long userId) {
        Route route = findSharedRoute(courseId);
        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;

        String userName = req.userName() != null
                ? req.userName()
                : (user != null ? user.getUserId() : "익명");

        RouteReview review = RouteReview.builder()
                .route(route)
                .user(user)
                .userName(userName)
                .rating(req.rating())
                .text(req.text())
                .build();
        reviewRepository.save(review);

        recalcRating(route);

        if (user != null) {
            followingNewsService.createNews(
                    userId,
                    FollowingNewsActionType.COURSE_COMPLETED,
                    route.getUuid(),
                    route.getTitle()
            );
        }

        return ReviewResponse.from(review);
    }

    // ──────────────────────────────────────────────────────────
    // 코스 즐겨찾기 저장 (내 루트 추가)
    // ──────────────────────────────────────────────────────────

    @Transactional
    public void saveCourse(String courseId, Long userId) {
        Route route = findSharedRoute(courseId);
        User user = findUser(userId);

        if (savedRouteRepository.existsByUserIdAndRouteId(userId, route.getId())) {
            throw new RouteException("이미 저장된 코스입니다.", HttpStatus.CONFLICT);
        }
        savedRouteRepository.save(SavedRoute.builder().user(user).route(route).build());
        followingNewsService.createNews(
                userId,
                FollowingNewsActionType.COURSE_SAVED,
                route.getUuid(),
                route.getTitle()
        );
    }

    // ──────────────────────────────────────────────────────────
    // 내 코스 목록 (내가 만든 + 저장한)
    // ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CoursePageResponse getMyCourses(Long userId, String q, String category,
                                           String region, String sort, int page, int size) {
        Sort sortSpec = resolveSort(null, sort);
        Pageable pageable = PageRequest.of(page, size, sortSpec);

        Page<Route> result = routeRepository.findMyCourses(
                userId, RouteStatus.DELETED, category, region, q, pageable);

        List<CourseItemResponse> items = result.getContent().stream()
                .map(this::toCourseItem)
                .toList();

        PageInfo pageInfo = new PageInfo(
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );

        return new CoursePageResponse(items, pageInfo);
    }

    // ──────────────────────────────────────────────────────────
    // 내 루트 생성 (프론트 stops/legs 포맷)
    // ──────────────────────────────────────────────────────────

    @Transactional
    public MyCourseDetailResponse createMyCourse(Long userId, CreateMyCourseRequest req) {
        User user = findUser(userId);

        int totalMinutes = req.legs() == null ? 0 :
                req.legs().stream().mapToInt(l -> l.minutes() != null ? l.minutes() : 0).sum();
        int totalMeters = req.legs() == null ? 0 :
                req.legs().stream().mapToInt(l -> l.distanceMeters() != null ? l.distanceMeters() : 0).sum();
        java.math.BigDecimal totalDistance = totalMeters > 0
                ? java.math.BigDecimal.valueOf(totalMeters).divide(java.math.BigDecimal.valueOf(1000), 2, java.math.RoundingMode.HALF_UP)
                : null;

        Route route = Route.builder()
                .uuid(java.util.UUID.randomUUID().toString())
                .user(user)
                .title(req.title())
                .status(RouteStatus.DRAFT)
                .isShared(Boolean.TRUE.equals(req.collaborative()))
                .estimatedTime(totalMinutes > 0 ? totalMinutes : null)
                .totalDistance(totalDistance)
                .build();
        routeRepository.save(route);

        List<RouteWaypoint> waypoints = buildWaypoints(route, req.stops());
        waypointRepository.saveAll(waypoints);

        List<RouteLeg> legs = buildLegs(route, req.legs());
        legRepository.saveAll(legs);

        return toMyCourseDetail(route, waypoints, legs);
    }

    // ──────────────────────────────────────────────────────────
    // 내 루트 상세 조회 (stops/legs 포맷)
    // ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public MyCourseDetailResponse getMyCourseDetail(Long userId, String courseId) {
        Route route = routeRepository.findByUuidAndStatusNot(courseId, RouteStatus.DELETED)
                .orElseThrow(() -> new RouteException("코스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (!route.getUser().getId().equals(userId)) {
            throw new RouteException("해당 코스에 접근할 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
        List<RouteWaypoint> waypoints = waypointRepository.findByRouteOrderBySequenceAsc(route);
        List<RouteLeg> legs = legRepository.findByRouteOrderBySequenceAsc(route);
        return toMyCourseDetail(route, waypoints, legs);
    }

    // ──────────────────────────────────────────────────────────
    // 내 코스 삭제
    // ──────────────────────────────────────────────────────────

    @Transactional
    public void deleteMyCourse(String courseId, Long userId) {
        Route route = routeRepository.findByUuidAndStatusNot(courseId, RouteStatus.DELETED)
                .orElseThrow(() -> new RouteException("코스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (!route.getUser().getId().equals(userId)) {
            // 직접 소유가 아니면 저장된 코스인지 확인 후 저장 취소
            Optional<SavedRoute> saved = savedRouteRepository.findByUserIdAndRouteId(userId, route.getId());
            if (saved.isPresent()) {
                savedRouteRepository.delete(saved.get());
                return;
            }
            throw new RouteException("해당 코스에 접근할 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
        route.markDeleted();
    }

    // ──────────────────────────────────────────────────────────
    // 내 루트 수정 (stops/legs 포맷)
    // ──────────────────────────────────────────────────────────

    @Transactional
    public MyCourseDetailResponse updateMyCourse(Long userId, String courseId, CreateMyCourseRequest req) {
        Route route = routeRepository.findByUuidAndStatusNot(courseId, RouteStatus.DELETED)
                .orElseThrow(() -> new RouteException("코스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (!route.getUser().getId().equals(userId)) {
            throw new RouteException("해당 코스에 접근할 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }

        int totalMinutes = req.legs() == null ? 0 :
                req.legs().stream().mapToInt(l -> l.minutes() != null ? l.minutes() : 0).sum();
        int totalMeters = req.legs() == null ? 0 :
                req.legs().stream().mapToInt(l -> l.distanceMeters() != null ? l.distanceMeters() : 0).sum();
        java.math.BigDecimal totalDistance = totalMeters > 0
                ? java.math.BigDecimal.valueOf(totalMeters).divide(java.math.BigDecimal.valueOf(1000), 2, java.math.RoundingMode.HALF_UP)
                : route.getTotalDistance();

        route.update(
                req.title() != null ? req.title() : route.getTitle(),
                route.getDescription(),
                totalDistance,
                totalMinutes > 0 ? totalMinutes : route.getEstimatedTime(),
                route.getThumbnailUrl(),
                route.getRegion(),
                route.getActivityType()
        );
        if (req.collaborative() != null) {
            if (Boolean.TRUE.equals(req.collaborative())) route.enableSharing();
            else route.disableSharing();
        }

        waypointRepository.deleteAllByRoute(route);
        legRepository.deleteAllByRoute(route);

        List<RouteWaypoint> waypoints = buildWaypoints(route, req.stops());
        waypointRepository.saveAll(waypoints);

        List<RouteLeg> legs = buildLegs(route, req.legs());
        legRepository.saveAll(legs);

        return toMyCourseDetail(route, waypoints, legs);
    }

    // ──────────────────────────────────────────────────────────
    // private helpers
    // ──────────────────────────────────────────────────────────

    private Route findSharedRoute(String courseId) {
        return routeRepository.findByUuidAndStatusNot(courseId, RouteStatus.DELETED)
                .filter(Route::isShared)
                .orElseThrow(() -> new RouteException("공유된 코스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RouteException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    private List<CourseStepResponse> loadSteps(Route route) {
        return waypointRepository.findByRouteOrderBySequenceAsc(route)
                .stream().map(CourseStepResponse::from).toList();
    }

    private CourseItemResponse toCourseItem(Route route) {
        List<CourseStepResponse> steps = loadSteps(route);
        return CourseItemResponse.of(route, steps);
    }

    private void recalcRating(Route route) {
        BigDecimal avg = reviewRepository.findAverageRatingByRoute(route);
        long count = reviewRepository.countByRoute(route);
        if (avg != null) {
            avg = avg.setScale(2, RoundingMode.HALF_UP);
        }
        route.updateRatingStats(avg, (int) count);
    }

    private List<RouteWaypoint> buildWaypoints(Route route, List<StopRequest> stops) {
        if (stops == null || stops.isEmpty()) return List.of();
        int seq = 1;
        List<RouteWaypoint> result = new java.util.ArrayList<>();
        for (StopRequest s : stops) {
            result.add(RouteWaypoint.builder()
                    .route(route)
                    .sequence(seq++)
                    .name(s.title())
                    .latitude(s.lat())
                    .longitude(s.lng())
                    .kind(s.kind())
                    .timeLine(s.timeLine())
                    .build());
        }
        return result;
    }

    private List<RouteLeg> buildLegs(Route route, List<LegRequest> legs) {
        if (legs == null || legs.isEmpty()) return List.of();
        int seq = 1;
        List<RouteLeg> result = new java.util.ArrayList<>();
        for (LegRequest l : legs) {
            result.add(RouteLeg.builder()
                    .route(route)
                    .sequence(seq++)
                    .mode(l.mode())
                    .minutes(l.minutes())
                    .transitType(l.transitType())
                    .directionsSummary(l.directionsSummary())
                    .directionsDetail(l.directionsDetail())
                    .distanceMeters(l.distanceMeters())
                    .build());
        }
        return result;
    }

    private MyCourseDetailResponse toMyCourseDetail(Route route, List<RouteWaypoint> waypoints, List<RouteLeg> legs) {
        List<StopResponse> stops = waypoints.stream().map(StopResponse::from).toList();
        List<LegResponse> legResponses = legs.stream().map(LegResponse::from).toList();
        return new MyCourseDetailResponse(route.getUuid(), route.getTitle(), route.isShared(), stops, legResponses);
    }

    private Sort resolveSort(String tab, String sort) {
        if ("popular".equals(tab) || "popular".equals(sort)) {
            return Sort.by(Sort.Direction.DESC, "views");
        }
        if ("rating".equals(sort)) {
            return Sort.by(Sort.Direction.DESC, "averageRating");
        }
        // date, all, 기본 → 최신순
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }
}
