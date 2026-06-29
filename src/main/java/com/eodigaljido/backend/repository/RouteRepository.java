package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.group.Group;
import com.eodigaljido.backend.domain.route.Route;
import com.eodigaljido.backend.domain.route.Route.RouteStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {

    Optional<Route> findByIdAndStatusNot(Long id, RouteStatus status);

    Optional<Route> findByUuidAndStatusNot(String uuid, RouteStatus status);

    List<Route> findByUserIdAndStatusNot(Long userId, RouteStatus status);

    List<Route> findByUserIdAndIsSharedTrueAndStatusNot(Long userId, RouteStatus status);

    List<Route> findByIsSharedTrueAndStatusNot(RouteStatus status);

    List<Route> findByStatusAndIsSharedAndDeletedAtIsNull(RouteStatus status, boolean isShared, Pageable pageable);

    @Query("SELECT r FROM Route r JOIN FETCH r.user WHERE r.isShared = true AND r.status <> :status")
    List<Route> findSharedRoutesWithUser(@Param("status") RouteStatus status);

    // 홈 인기 코스 (상위 N개)
    @Query("SELECT r FROM Route r JOIN FETCH r.user WHERE r.isShared = true AND r.status <> :status")
    List<Route> findSharedCoursesForHome(@Param("status") RouteStatus status, Pageable pageable);

    // 공유 코스 목록 (필터/검색/페이징)
    @Query("""
            SELECT r FROM Route r JOIN FETCH r.user u
            WHERE r.isShared = true AND r.status <> :status
              AND (:category IS NULL OR r.activityType = :category)
              AND (:region IS NULL OR r.region = :region)
              AND (:q IS NULL OR r.title LIKE %:q% OR r.description LIKE %:q%)
              AND (:nickname IS NULL OR EXISTS (
                    SELECT p FROM Profile p WHERE p.user = u AND p.nickname LIKE %:nickname%
                  ))
            """)
    Page<Route> findSharedCourses(@Param("status") RouteStatus status,
                                  @Param("category") String category,
                                  @Param("region") String region,
                                  @Param("q") String q,
                                  @Param("nickname") String nickname,
                                  Pageable pageable);

    // 친구들의 공유 코스 목록
    @Query("""
            SELECT r FROM Route r JOIN FETCH r.user u
            WHERE r.isShared = true AND r.status <> :status
              AND (EXISTS (
                    SELECT f FROM Friend f
                    WHERE f.status = 'ACCEPTED'
                      AND ((f.requester.id = :userId AND f.receiver.id = u.id)
                        OR (f.receiver.id = :userId AND f.requester.id = u.id))
                  ))
              AND (:category IS NULL OR r.activityType = :category)
              AND (:region IS NULL OR r.region = :region)
              AND (:q IS NULL OR r.title LIKE %:q% OR r.description LIKE %:q%)
              AND (:nickname IS NULL OR EXISTS (
                    SELECT p FROM Profile p WHERE p.user = u AND p.nickname LIKE %:nickname%
                  ))
            """)
    Page<Route> findSharedCoursesByFriends(@Param("userId") Long userId,
                                           @Param("status") RouteStatus status,
                                           @Param("category") String category,
                                           @Param("region") String region,
                                           @Param("q") String q,
                                           @Param("nickname") String nickname,
                                           Pageable pageable);

    // 내 코스 목록 (직접 만들거나 저장한 코스, 또는 공동작업 모임 루트)
    @Query("""
            SELECT DISTINCT r FROM Route r JOIN FETCH r.user
            WHERE r.status <> :status
              AND (r.user.id = :userId
                OR EXISTS (
                     SELECT s FROM SavedRoute s
                     WHERE s.route = r AND s.user.id = :userId
                   )
                OR (r.isCollaborative = true
                    AND (EXISTS (
                          SELECT cm FROM CourseMember cm
                          WHERE cm.route = r
                            AND cm.user.id = :userId
                            AND cm.leftAt IS NULL
                        )
                        OR (r.group IS NOT NULL
                            AND EXISTS (
                                  SELECT gm FROM GroupMember gm
                                  WHERE gm.group = r.group
                                    AND gm.user.id = :userId
                                    AND gm.leftAt IS NULL
                                )))))
              AND (:category IS NULL OR r.activityType = :category)
              AND (:region IS NULL OR r.region = :region)
              AND (:q IS NULL OR r.title LIKE %:q% OR r.description LIKE %:q%)
            """)
    Page<Route> findMyCourses(@Param("userId") Long userId,
                              @Param("status") RouteStatus status,
                              @Param("category") String category,
                              @Param("region") String region,
                              @Param("q") String q,
                              Pageable pageable);

    // 유저 프로필용: 공유 코스 수
    long countByUserIdAndIsSharedTrueAndStatusNot(Long userId, RouteStatus status);

    // 유저 프로필용: 공유 코스들의 평균 평점
    @Query("SELECT AVG(r.averageRating) FROM Route r WHERE r.user.id = :userId AND r.isShared = true AND r.status <> :status AND r.averageRating IS NOT NULL")
    java.math.BigDecimal findAverageRatingOfSharedRoutesByUserId(@Param("userId") Long userId, @Param("status") RouteStatus status);

    // 루트 채팅방으로 연결된 Route 조회
    Optional<Route> findByChatRoomIdAndStatusNot(Long chatRoomId, RouteStatus status);

    // 모임 내 루트 목록
    @Query("SELECT r FROM Route r JOIN FETCH r.user WHERE r.group = :group AND r.status <> :status ORDER BY r.createdAt DESC")
    List<Route> findByGroupAndStatusNot(@Param("group") Group group, @Param("status") RouteStatus status);
}
