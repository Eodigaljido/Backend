package com.eodigaljido.backend.service;

import com.eodigaljido.backend.domain.chat.ChatRoom;
import com.eodigaljido.backend.domain.chat.ChatRoomMember;
import com.eodigaljido.backend.domain.group.Group;
import com.eodigaljido.backend.domain.notification.NotificationType;
import com.eodigaljido.backend.domain.route.Route;
import com.eodigaljido.backend.domain.route.Route.RouteStatus;
import com.eodigaljido.backend.domain.route.RouteLeg;
import com.eodigaljido.backend.domain.route.RouteReview;
import com.eodigaljido.backend.domain.route.RouteWaypoint;
import com.eodigaljido.backend.domain.route.RouteTag;
import com.eodigaljido.backend.domain.route.SavedRoute;
import com.eodigaljido.backend.domain.following.FollowingNewsActionType;
import com.eodigaljido.backend.domain.user.User;
import com.eodigaljido.backend.dto.course.*;
import com.eodigaljido.backend.event.NotificationEvent;
import com.eodigaljido.backend.repository.RouteLegRepository;
import com.eodigaljido.backend.exception.RouteException;
import com.eodigaljido.backend.exception.UserException;
import com.eodigaljido.backend.repository.*;
import com.eodigaljido.backend.exception.GroupException;
import org.springframework.context.ApplicationEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseService {

    private static final long COURSE_THUMBNAIL_MAX_BYTES = 10L * 1024 * 1024;
    private static final Set<String> COURSE_THUMBNAIL_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );

    private final RouteRepository routeRepository;
    private final RouteWaypointRepository waypointRepository;
    private final RouteLegRepository legRepository;
    private final RouteReviewRepository reviewRepository;
    private final SavedRouteRepository savedRouteRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final OnboardingAnswerRepository onboardingAnswerRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final FollowingNewsService followingNewsService;
    private final ApplicationEventPublisher eventPublisher;
    private final FileStorageService fileStorageService;

    // ──────────────────────────────────────────────────────────
    // 코스 공유 링크 preview (비로그인 허용, 조회수 증가 없음)
    // ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CoursePreviewResponse getCoursePreview(String courseId) {
        Route route = findSharedRoute(courseId);

        List<RouteWaypoint> waypoints = waypointRepository.findByRouteOrderBySequenceAsc(route);
        String departure = waypoints.isEmpty() ? null : waypoints.get(0).getName();
        String arrival = waypoints.size() < 2 ? departure : waypoints.get(waypoints.size() - 1).getName();

        List<String> tags = route.getTags() != null
                ? route.getTags().stream().limit(4).toList()
                : List.of();

        long saveCount = savedRouteRepository.countByRouteId(route.getId());

        String authorNickname = profileRepository.findByUser(route.getUser())
                .map(p -> p.getNickname())
                .orElse(route.getUser().getUserId());

        return CoursePreviewResponse.of(route, tags, departure, arrival, saveCount, authorNickname);
    }

    // ──────────────────────────────────────────────────────────
    // 홈 코스 목록 (인기/최근 공유 코스)
    // ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CourseItemResponse> getHomeCourses(int limit, Long userId) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "views"));
        List<Route> routes = routeRepository.findSharedCoursesForHome(RouteStatus.DELETED, pageable);
        Set<Long> savedIds = batchSavedIds(userId, routes);
        return routes.stream()
                .map(r -> toCourseItem(r, savedIds.contains(r.getId())))
                .toList();
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

        List<Route> routes = result.getContent();
        Set<Long> savedIds = batchSavedIds(currentUserId, routes);
        List<CourseItemResponse> items = routes.stream()
                .map(r -> toCourseItem(r, savedIds.contains(r.getId())))
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
    public CourseDetailResponse getCourseDetail(String courseId, Long userId) {
        Route route = findSharedRoute(courseId);
        route.incrementViews();

        List<CourseStepResponse> steps = loadSteps(route);
        List<ReviewResponse> reviews = reviewRepository.findByRouteOrderByCreatedAtDesc(route)
                .stream().map(ReviewResponse::from).toList();
        boolean savedByMe = userId != null && savedRouteRepository.existsByUserIdAndRouteId(userId, route.getId());

        return CourseDetailResponse.of(route, steps, reviews, savedByMe);
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
    // 개인 루트 생성 (stops/legs 포맷)
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
                .estimatedTime(totalMinutes > 0 ? totalMinutes : null)
                .totalDistance(totalDistance)
                .build();
        routeRepository.save(route);
        route.updateTags(processTags(req.tags()));

        List<RouteWaypoint> waypoints = buildWaypoints(route, req.stops());
        waypointRepository.saveAll(waypoints);

        List<RouteLeg> legs = buildLegs(route, req.legs());
        legRepository.saveAll(legs);

        return toMyCourseDetail(route, waypoints, legs);
    }

    // ──────────────────────────────────────────────────────────
    // 모임 루트 생성 (stops/legs 포맷, 채팅방 자동 생성)
    // ──────────────────────────────────────────────────────────

    @Transactional
    public MyCourseDetailResponse createGroupCourse(Long userId, CreateGroupCourseRequest req) {
        User user = findUser(userId);

        Group group = groupRepository.findByUuidAndStatusNot(req.groupUuid(), Group.GroupStatus.DELETED)
                .orElseThrow(() -> new RouteException("모임을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (!groupMemberRepository.existsByGroupAndUserAndLeftAtIsNull(group, user)) {
            throw new RouteException("모임 멤버가 아닙니다.", HttpStatus.FORBIDDEN);
        }

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
                .estimatedTime(totalMinutes > 0 ? totalMinutes : null)
                .totalDistance(totalDistance)
                .build();
        routeRepository.save(route);
        route.updateTags(processTags(req.tags()));
        route.assignGroup(group);
        route.enableCollaboration();

        ChatRoom routeRoom = ChatRoom.builder()
                .uuid(java.util.UUID.randomUUID().toString())
                .name(req.title())
                .type(ChatRoom.RoomType.ROUTE)
                .createdBy(user)
                .build();
        routeRoom.assignGroup(group);
        chatRoomRepository.save(routeRoom);
        chatRoomMemberRepository.save(ChatRoomMember.builder()
                .room(routeRoom)
                .user(user)
                .role(ChatRoomMember.MemberRole.ADMIN)
                .build());
        route.assignChatRoom(routeRoom);

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
        if (!canEditRoute(route, userId)) {
            throw new RouteException("해당 코스에 접근할 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
        List<RouteWaypoint> waypoints = waypointRepository.findByRouteOrderBySequenceAsc(route);
        List<RouteLeg> legs = legRepository.findByRouteOrderBySequenceAsc(route);
        return toMyCourseDetail(route, waypoints, legs);
    }

    // ──────────────────────────────────────────────────────────
    // 내 루트 대표 이미지
    // ──────────────────────────────────────────────────────────

    @Transactional
    public CourseThumbnailResponse updateMyCourseThumbnail(Long userId, String courseId, MultipartFile image) {
        Route route = findMyOwnedRoute(userId, courseId);
        validateCourseThumbnail(image);

        String oldUrl = route.getThumbnailUrl();
        String newUrl;
        try {
            newUrl = fileStorageService.store(
                    image,
                    "courses/" + route.getUuid(),
                    "thumbnail-" + UUID.randomUUID()
            );
        } catch (IllegalStateException e) {
            throw new RouteException("파일 저장소 설정이 누락되었습니다. " + e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        } catch (IllegalArgumentException e) {
            throw new RouteException(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            throw new RouteException("대표 이미지 업로드 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        route.updateThumbnailUrl(newUrl);
        if (oldUrl != null && !oldUrl.equals(newUrl)) {
            fileStorageService.delete(oldUrl);
        }
        return new CourseThumbnailResponse(newUrl);
    }

    @Transactional
    public CourseThumbnailResponse deleteMyCourseThumbnail(Long userId, String courseId) {
        Route route = findMyOwnedRoute(userId, courseId);
        String oldUrl = route.getThumbnailUrl();
        route.updateThumbnailUrl(null);
        fileStorageService.delete(oldUrl);
        return new CourseThumbnailResponse(null);
    }

    // ──────────────────────────────────────────────────────────
    // 내 코스 삭제
    // ──────────────────────────────────────────────────────────

    @Transactional
    public void deleteMyCourse(String courseId, Long userId) {
        Route route = routeRepository.findByUuidAndStatusNot(courseId, RouteStatus.DELETED)
                .orElseThrow(() -> new RouteException("코스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        boolean isOwner = route.getUser().getId().equals(userId);

        if (!isOwner) {
            // 그룹 루트: 방장만 삭제 가능
            if (route.getGroup() != null) {
                if (!route.getGroup().getCreatedBy().getId().equals(userId)) {
                    throw new RouteException("그룹 루트는 소유자 또는 방장만 삭제할 수 있습니다.", HttpStatus.FORBIDDEN);
                }
                route.markDeleted();
                return;
            }
            // 개인 루트: 저장 취소로 처리
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
        if (!canEditRoute(route, userId)) {
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
        if (req.tags() != null) {
            route.updateTags(processTags(req.tags()));
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
    // 상태 변경 (DRAFT / PUBLISHED)
    // ──────────────────────────────────────────────────────────

    @Transactional
    public MyCourseDetailResponse updateCourseStatus(Long userId, String courseId, RouteStatus status) {
        if (status == RouteStatus.DELETED) {
            throw new RouteException("상태를 DELETED로 변경할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
        Route route = findMyOwnedRoute(userId, courseId);
        route.updateStatus(status);
        if (status == RouteStatus.PUBLISHED) {
            route.markPublishedBy(route.getUser());
        }
        List<RouteWaypoint> waypoints = waypointRepository.findByRouteOrderBySequenceAsc(route);
        List<RouteLeg> legs = legRepository.findByRouteOrderBySequenceAsc(route);
        return toMyCourseDetail(route, waypoints, legs);
    }

    // ──────────────────────────────────────────────────────────
    // 내가 공유 중인 코스 목록
    // ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CourseItemResponse> getSharingCourses(Long userId) {
        return routeRepository.findByUserIdAndIsSharedTrueAndStatusNot(userId, RouteStatus.DELETED)
                .stream()
                .map(this::toCourseItem)
                .toList();
    }

    // ──────────────────────────────────────────────────────────
    // 즐겨찾기(저장)한 코스 목록 조회
    // ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CourseItemResponse> getMySavedCourses(Long userId) {
        return savedRouteRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(sr -> sr.getRoute())
                .filter(r -> r.getStatus() != RouteStatus.DELETED)
                .map(r -> toCourseItem(r, true))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CourseItemResponse> getUserSavedCourses(String uuid) {
        User user = userRepository.findByUuid(uuid)
                .filter(u -> u.getStatus() == User.UserStatus.ACTIVE)
                .orElseThrow(() -> new UserException("존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND));
        return savedRouteRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(sr -> sr.getRoute())
                .filter(r -> r.getStatus() != RouteStatus.DELETED)
                .map(this::toCourseItem)
                .toList();
    }

    // ──────────────────────────────────────────────────────────
    // 저장한 코스 목록 (페이징, 본인 또는 타인)
    // ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CoursePageResponse getSavedCoursesPage(Long requesterId, String userUuid,
                                                   String sort, int page, int size) {
        Long targetUserId;
        if (userUuid != null && !userUuid.isBlank()) {
            User target = userRepository.findByUuid(userUuid)
                    .filter(u -> u.getStatus() == User.UserStatus.ACTIVE)
                    .orElseThrow(() -> new UserException("존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND));
            targetUserId = target.getId();
        } else {
            targetUserId = requesterId;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SavedRoute> result = savedRouteRepository
                .findByUserIdAndRouteStatusNotOrderByCreatedAtDesc(targetUserId, RouteStatus.DELETED, pageable);

        List<Route> routes = result.getContent().stream().map(SavedRoute::getRoute).toList();
        Set<Long> savedIds = batchSavedIds(requesterId, routes);
        List<CourseItemResponse> items = routes.stream()
                .map(r -> toCourseItem(r, savedIds.contains(r.getId())))
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
    // 공유 활성화 / 비활성화
    // ──────────────────────────────────────────────────────────

    @Transactional
    public void enableSharing(Long userId, String courseId) {
        Route route = findMyOwnedRoute(userId, courseId);
        boolean wasShared = route.isShared();
        route.enableSharing(route.getUser());

        if (!wasShared) {
            followingNewsService.createNews(
                    userId,
                    FollowingNewsActionType.COURSE_PUBLISHED,
                    route.getUuid(),
                    route.getTitle()
            );
        }

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
    public void disableSharing(Long userId, String courseId) {
        Route route = findMyOwnedRoute(userId, courseId);
        route.disableSharing();
    }

    // ──────────────────────────────────────────────────────────
    // 저장 취소
    // ──────────────────────────────────────────────────────────

    @Transactional
    public void unsaveCourse(Long userId, String courseId) {
        Route route = routeRepository.findByUuidAndStatusNot(courseId, RouteStatus.DELETED)
                .orElseThrow(() -> new RouteException("코스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        SavedRoute saved = savedRouteRepository.findByUserIdAndRouteId(userId, route.getId())
                .orElseThrow(() -> new RouteException("저장된 코스가 아닙니다.", HttpStatus.NOT_FOUND));
        savedRouteRepository.delete(saved);
    }

    // ──────────────────────────────────────────────────────────
    // 코스 복사
    // ──────────────────────────────────────────────────────────

    @Transactional
    public MyCourseDetailResponse copyCourse(Long userId, String courseId) {
        Route original = routeRepository.findByUuidAndStatusNot(courseId, RouteStatus.DELETED)
                .orElseThrow(() -> new RouteException("코스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (!original.isShared() && !original.getUser().getId().equals(userId)) {
            throw new RouteException("공유되지 않은 코스는 복사할 수 없습니다.", HttpStatus.FORBIDDEN);
        }

        User user = findUser(userId);

        Route copied = Route.builder()
                .uuid(java.util.UUID.randomUUID().toString())
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
        copied.inheritForkMetadataFrom(original);
        copied.updateTags(new java.util.ArrayList<>(original.getTags()));

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
                        .stayMinutes(w.getStayMinutes())
                        .kind(w.getKind())
                        .timeLine(w.getTimeLine())
                        .build())
                .toList();
        waypointRepository.saveAll(waypoints);

        List<RouteLeg> legs = legRepository.findByRouteOrderBySequenceAsc(original)
                .stream()
                .map(l -> RouteLeg.builder()
                        .route(copied)
                        .sequence(l.getSequence())
                        .mode(l.getMode())
                        .minutes(l.getMinutes())
                        .transitType(l.getTransitType())
                        .directionsSummary(l.getDirectionsSummary())
                        .directionsDetail(l.getDirectionsDetail())
                        .distanceMeters(l.getDistanceMeters())
                        .build())
                .toList();
        legRepository.saveAll(legs);

        if (!original.getUser().getId().equals(userId)) {
            eventPublisher.publishEvent(NotificationEvent.of(
                    original.getUser().getId(), userId,
                    NotificationType.ROUTE_USED,
                    "코스 사용",
                    user.getUserId() + "님이 회원님의 코스를 사용했습니다: " + original.getTitle(),
                    original.getUuid(), "ROUTE"
            ));
        }

        return toMyCourseDetail(copied, waypoints, legs);
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
        return toCourseItem(route, false);
    }

    private CourseItemResponse toCourseItem(Route route, boolean savedByMe) {
        List<CourseStepResponse> steps = loadSteps(route);
        return CourseItemResponse.of(route, steps, savedByMe);
    }

    private Set<Long> batchSavedIds(Long userId, List<Route> routes) {
        if (userId == null || routes.isEmpty()) return Set.of();
        List<Long> routeIds = routes.stream().map(Route::getId).toList();
        return savedRouteRepository.findSavedRouteIdsByUserId(userId, routeIds);
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
            validateStop(s, seq);
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

    private void validateStop(StopRequest stop, int sequence) {
        if (stop == null) {
            throw new RouteException("경유지 정보가 비어 있습니다. sequence=" + sequence, HttpStatus.BAD_REQUEST);
        }
        if (stop.lat() == null || stop.lng() == null) {
            throw new RouteException("경유지 좌표는 필수입니다. sequence=" + sequence, HttpStatus.BAD_REQUEST);
        }
        if (stop.lat().compareTo(BigDecimal.valueOf(-90)) < 0 || stop.lat().compareTo(BigDecimal.valueOf(90)) > 0) {
            throw new RouteException("경유지 위도 값이 올바르지 않습니다. sequence=" + sequence, HttpStatus.BAD_REQUEST);
        }
        if (stop.lng().compareTo(BigDecimal.valueOf(-180)) < 0 || stop.lng().compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new RouteException("경유지 경도 값이 올바르지 않습니다. sequence=" + sequence, HttpStatus.BAD_REQUEST);
        }
    }

    private void validateCourseThumbnail(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new RouteException("대표 이미지 파일은 필수입니다.", HttpStatus.BAD_REQUEST);
        }
        if (image.getSize() > COURSE_THUMBNAIL_MAX_BYTES) {
            throw new RouteException("대표 이미지 파일은 10MB를 초과할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
        String contentType = image.getContentType();
        if (contentType == null || !COURSE_THUMBNAIL_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new RouteException("대표 이미지는 JPEG, PNG, WebP 형식만 업로드할 수 있습니다.", HttpStatus.BAD_REQUEST);
        }
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
        List<String> tags = route.getTags() != null ? List.copyOf(route.getTags()) : List.of();
        String chatRoomUuid = route.getChatRoom() != null ? route.getChatRoom().getUuid() : null;
        boolean collaborative = route.getGroup() != null;
        return new MyCourseDetailResponse(
                route.getUuid(),
                route.getTitle(),
                route.getThumbnailUrl(),
                collaborative,
                chatRoomUuid,
                stops,
                legResponses,
                tags,
                route.getForkSourceCourseId(),
                route.getRootForkSourceCourseId(),
                route.getOriginalAuthorUuid() != null ? route.getOriginalAuthorUuid() : route.getUser().getUuid(),
                route.getOriginalAuthorUserId() != null ? route.getOriginalAuthorUserId() : route.getUser().getUserId(),
                route.getModifierUuid(),
                route.getModifierUserId(),
                true
        );
    }

    private List<String> processTags(List<RouteTag> tags) {
        if (tags == null) return List.of();
        return tags.stream()
                .filter(java.util.Objects::nonNull)
                .limit(2)
                .map(Enum::name)
                .toList();
    }

    private Sort resolveSort(String tab, String sort) {
        if ("popular".equals(tab) || "popular".equals(sort)) {
            return Sort.by(Sort.Direction.DESC, "views");
        }
        if ("rating".equals(sort)) {
            return Sort.by(Sort.Direction.DESC, "averageRating");
        }
        return Sort.by(Sort.Direction.DESC, "createdAt");
    }

    private Route findMyOwnedRoute(Long userId, String courseId) {
        Route route = routeRepository.findByUuidAndStatusNot(courseId, RouteStatus.DELETED)
                .orElseThrow(() -> new RouteException("코스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (!route.getUser().getId().equals(userId)) {
            throw new RouteException("해당 코스에 접근할 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
        return route;
    }

    private boolean canEditRoute(Route route, Long userId) {
        if (route.getUser().getId().equals(userId)) return true;
        if (route.getGroup() == null) return false;
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return false;
        return groupMemberRepository.existsByGroupAndUserAndLeftAtIsNull(route.getGroup(), user);
    }
}
