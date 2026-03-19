package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.friend.Friend;
import com.eodigaljido.backend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendRepository extends JpaRepository<Friend, Long> {

    // 두 유저 사이의 친구 관계 조회 (방향 무관)
    @Query("SELECT f FROM Friend f WHERE (f.requester = :a AND f.receiver = :b) OR (f.requester = :b AND f.receiver = :a)")
    Optional<Friend> findBetween(@Param("a") User a, @Param("b") User b);

    // 수락된 친구 목록 조회
    @Query("SELECT f FROM Friend f WHERE (f.requester = :user OR f.receiver = :user) AND f.status = 'ACCEPTED'")
    List<Friend> findAcceptedFriends(@Param("user") User user);

    // 받은 대기 중인 요청 목록
    List<Friend> findByReceiverAndStatus(User receiver, Friend.FriendStatus status);

    // 보낸 대기 중인 요청 목록
    List<Friend> findByRequesterAndStatus(User requester, Friend.FriendStatus status);

    // 특정 친구 관계 조회 (수락된 상태)
    @Query("SELECT f FROM Friend f WHERE f.id = :id AND (f.requester = :user OR f.receiver = :user) AND f.status = 'ACCEPTED'")
    Optional<Friend> findAcceptedById(@Param("id") Long id, @Param("user") User user);
}
