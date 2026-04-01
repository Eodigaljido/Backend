package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.chat.ChatRoom;
import com.eodigaljido.backend.domain.chat.ChatRoomMember;
import com.eodigaljido.backend.domain.user.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    Optional<ChatRoomMember> findByRoomAndUserAndLeftAtIsNull(ChatRoom room, User user);

    List<ChatRoomMember> findByRoomAndLeftAtIsNull(ChatRoom room);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.room = :room " +
           "AND m.createdAt > :since AND m.isDeleted = false")
    long countMessagesAfter(@Param("room") ChatRoom room, @Param("since") LocalDateTime since);
}
