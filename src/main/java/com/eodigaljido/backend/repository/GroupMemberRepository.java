package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.group.Group;
import com.eodigaljido.backend.domain.group.GroupMember;
import com.eodigaljido.backend.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    Optional<GroupMember> findByGroupAndUserAndLeftAtIsNull(Group group, User user);

    Optional<GroupMember> findByGroupAndUser(Group group, User user);

    boolean existsByGroupAndUserAndLeftAtIsNull(Group group, User user);

    @Query("SELECT gm FROM GroupMember gm JOIN FETCH gm.user WHERE gm.group = :group AND gm.leftAt IS NULL")
    Page<GroupMember> findActiveMembers(@Param("group") Group group, Pageable pageable);

    long countByGroupAndLeftAtIsNull(Group group);
}
