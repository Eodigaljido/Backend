package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.group.Group;
import com.eodigaljido.backend.domain.group.GroupJoinRequest;
import com.eodigaljido.backend.domain.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface GroupJoinRequestRepository extends JpaRepository<GroupJoinRequest, Long> {

    Optional<GroupJoinRequest> findByGroupAndRequesterAndStatus(
            Group group, User requester, GroupJoinRequest.RequestStatus status);

    boolean existsByGroupAndRequesterAndStatus(
            Group group, User requester, GroupJoinRequest.RequestStatus status);

    @Query("SELECT jr FROM GroupJoinRequest jr JOIN FETCH jr.requester " +
           "WHERE jr.group = :group AND jr.status = 'PENDING' ORDER BY jr.createdAt ASC")
    Page<GroupJoinRequest> findPendingRequests(@Param("group") Group group, Pageable pageable);
}
