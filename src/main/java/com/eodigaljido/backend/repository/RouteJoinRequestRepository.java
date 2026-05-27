package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.chat.ChatRoom;
import com.eodigaljido.backend.domain.route.RouteJoinRequest;
import com.eodigaljido.backend.domain.route.RouteJoinRequest.RequestStatus;
import com.eodigaljido.backend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RouteJoinRequestRepository extends JpaRepository<RouteJoinRequest, Long> {

    @Query("SELECT r FROM RouteJoinRequest r JOIN FETCH r.requester WHERE r.chatRoom = :chatRoom AND r.status = :status")
    List<RouteJoinRequest> findByChatRoomAndStatus(@Param("chatRoom") ChatRoom chatRoom,
                                                   @Param("status") RequestStatus status);

    Optional<RouteJoinRequest> findByChatRoomAndRequester(ChatRoom chatRoom, User requester);

    boolean existsByChatRoomAndRequesterAndStatus(ChatRoom chatRoom, User requester, RequestStatus status);
}
