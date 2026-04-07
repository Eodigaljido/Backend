package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.chat.ChatMessage;
import com.eodigaljido.backend.domain.chat.ChatRoom;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Optional<ChatMessage> findByUuid(String uuid);

    List<ChatMessage> findByRoomOrderByCreatedAtDesc(ChatRoom room, Pageable pageable);

    @Query("SELECT m FROM ChatMessage m WHERE m.room = :room AND m.id < :beforeId ORDER BY m.createdAt DESC")
    List<ChatMessage> findByRoomBeforeId(@Param("room") ChatRoom room, @Param("beforeId") Long beforeId, Pageable pageable);

    Optional<ChatMessage> findTopByRoomOrderByCreatedAtDesc(ChatRoom room);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.room = :room AND m.isDeleted = false")
    long countActiveByRoom(@Param("room") ChatRoom room);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.room = :room " +
           "AND m.createdAt > :since AND m.isDeleted = false")
    long countMessagesAfter(@Param("room") ChatRoom room, @Param("since") java.time.LocalDateTime since);
}
