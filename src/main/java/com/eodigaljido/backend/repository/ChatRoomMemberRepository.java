package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.chat.ChatRoom;
import com.eodigaljido.backend.domain.chat.ChatRoomMember;
import com.eodigaljido.backend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    Optional<ChatRoomMember> findByRoomAndUserAndLeftAtIsNull(ChatRoom room, User user);

    @Query("SELECT m FROM ChatRoomMember m JOIN FETCH m.user WHERE m.room = :room AND m.leftAt IS NULL")
    List<ChatRoomMember> findByRoomAndLeftAtIsNull(@Param("room") ChatRoom room);

    @Query("SELECT m FROM ChatRoomMember m JOIN FETCH m.user WHERE m.room IN :rooms AND m.leftAt IS NULL")
    List<ChatRoomMember> findByRoomInAndLeftAtIsNull(@Param("rooms") List<ChatRoom> rooms);
}
