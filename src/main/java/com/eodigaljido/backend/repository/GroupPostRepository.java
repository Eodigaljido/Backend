package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.group.Group;
import com.eodigaljido.backend.domain.group.GroupPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GroupPostRepository extends JpaRepository<GroupPost, Long> {

    Optional<GroupPost> findByUuidAndDeletedAtIsNull(String uuid);

    @Query("SELECT p FROM GroupPost p JOIN FETCH p.author " +
           "WHERE p.group = :group AND p.deletedAt IS NULL ORDER BY p.createdAt DESC")
    Page<GroupPost> findByGroupAndDeletedAtIsNull(@Param("group") Group group, Pageable pageable);
}
