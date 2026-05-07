package com.eodigaljido.backend.repository;

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
            SELECT r FROM Route r JOIN FETCH r.user
            WHERE r.isShared = true AND r.status <> :status
              AND (:category IS NULL OR r.activityType = :category)
              AND (:region IS NULL OR r.region = :region)
              AND (:q IS NULL OR r.title LIKE %:q% OR r.description LIKE %:q%)
            """)
    Page<Route> findSharedCourses(@Param("status") RouteStatus status,
                                  @Param("category") String category,
                                  @Param("region") String region,
                                  @Param("q") String q,
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
            """)
    Page<Route> findSharedCoursesByFriends(@Param("userId") Long userId,
                                           @Param("status") RouteStatus status,
                                           @Param("category") String category,
                                           @Param("region") String region,
                                           @Param("q") String q,
                                           Pageable pageable);

    // 내 코스 목록 (직접 만들거나 저장한 코스)
    @Query("""
            SELECT DISTINCT r FROM Route r JOIN FETCH r.user
            WHERE r.status <> :status
              AND (r.user.id = :userId
                OR EXISTS (
                     SELECT s FROM SavedRoute s
                     WHERE s.route = r AND s.user.id = :userId
                   ))
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
}
