package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.chat.ChatRoom;
import com.eodigaljido.backend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT r FROM ChatRoom r JOIN FETCH r.createdBy WHERE r.uuid = :uuid AND r.deletedAt IS NULL")
    Optional<ChatRoom> findByUuidAndDeletedAtIsNull(@Param("uuid") String uuid);

    @Query("SELECT r FROM ChatRoom r JOIN FETCH r.createdBy JOIN ChatRoomMember m ON m.room = r " +
           "WHERE m.user = :user AND m.leftAt IS NULL AND r.deletedAt IS NULL " +
           "ORDER BY r.createdAt DESC")
    List<ChatRoom> findRoomsForUser(@Param("user") User user);
}
