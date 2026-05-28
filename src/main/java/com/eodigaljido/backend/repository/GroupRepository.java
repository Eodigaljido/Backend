package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.group.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {

    Optional<Group> findByUuidAndStatusNot(String uuid, Group.GroupStatus status);

    @Query("SELECT g FROM Group g WHERE g.status = 'ACTIVE' AND g.type = 'PUBLIC' ORDER BY g.viewCount DESC")
    Page<Group> findPublicGroupsOrderByViewCount(Pageable pageable);

    @Query("SELECT g FROM Group g WHERE g.status = 'ACTIVE' AND " +
           "(g.type = 'PUBLIC' OR :showPrivate = true) AND " +
           "LOWER(g.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Group> searchByName(@Param("keyword") String keyword,
                              @Param("showPrivate") boolean showPrivate,
                              Pageable pageable);
}
