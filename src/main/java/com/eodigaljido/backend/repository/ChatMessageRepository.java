package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.chat.ChatMessage;
import com.eodigaljido.backend.domain.chat.ChatRoom;
import com.eodigaljido.backend.domain.route.Route;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Optional<ChatMessage> findByUuid(String uuid);

    @Query("SELECT m FROM ChatMessage m JOIN FETCH m.sender WHERE m.route = :route ORDER BY m.createdAt ASC")
    Page<ChatMessage> findByRouteOrderByCreatedAtAsc(@Param("route") Route route, Pageable pageable);

    List<ChatMessage> findByRoomOrderByCreatedAtDesc(ChatRoom room, Pageable pageable);

    @Query("SELECT m FROM ChatMessage m WHERE m.room = :room AND m.id < :beforeId ORDER BY m.createdAt DESC")
    List<ChatMessage> findByRoomBeforeId(@Param("room") ChatRoom room, @Param("beforeId") Long beforeId, Pageable pageable);

    Optional<ChatMessage> findTopByRoomOrderByCreatedAtDesc(ChatRoom room);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.room = :room")
    long countActiveByRoom(@Param("room") ChatRoom room);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.room = :room AND m.createdAt > :since")
    long countMessagesAfter(@Param("room") ChatRoom room, @Param("since") java.time.LocalDateTime since);

    // 나와 친구가 함께 속한 채팅방의 가장 최근 메시지 시각을 친구별로 집계
    @Query("""
        SELECT crm.user.id, MAX(m.createdAt)
        FROM ChatRoomMember crm
        JOIN ChatMessage m ON m.room = crm.room
        WHERE crm.room IN (
            SELECT crm2.room FROM ChatRoomMember crm2
            WHERE crm2.user.id = :userId
            AND crm2.leftAt IS NULL
            AND crm2.room.deletedAt IS NULL
        )
        AND crm.leftAt IS NULL
        AND crm.user.id IN :friendIds
        GROUP BY crm.user.id
        ORDER BY MAX(m.createdAt) DESC
    """)
    List<Object[]> findRecentContactedFriends(
            @Param("userId") Long userId,
            @Param("friendIds") List<Long> friendIds
    );
}
