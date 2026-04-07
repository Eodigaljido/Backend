package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.chat.ChatRoom;
import com.eodigaljido.backend.domain.chat.ChatRoomMember;
import com.eodigaljido.backend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    Optional<ChatRoomMember> findByRoomAndUserAndLeftAtIsNull(ChatRoom room, User user);

    List<ChatRoomMember> findByRoomAndLeftAtIsNull(ChatRoom room);

    List<ChatRoomMember> findByRoomInAndLeftAtIsNull(List<ChatRoom> rooms);
}
