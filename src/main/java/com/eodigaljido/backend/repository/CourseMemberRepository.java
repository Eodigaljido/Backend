package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.route.CourseMember;
import com.eodigaljido.backend.domain.route.Route;
import com.eodigaljido.backend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseMemberRepository extends JpaRepository<CourseMember, Long> {

    Optional<CourseMember> findByRouteAndUserAndLeftAtIsNull(Route route, User user);

    Optional<CourseMember> findByRouteAndUser(Route route, User user);

    boolean existsByRouteAndUserAndLeftAtIsNull(Route route, User user);

    @Query("SELECT m FROM CourseMember m JOIN FETCH m.user WHERE m.route = :route AND m.leftAt IS NULL ORDER BY m.createdAt ASC")
    List<CourseMember> findByRouteAndLeftAtIsNull(@Param("route") Route route);

    long countByRoute(Route route);
}
