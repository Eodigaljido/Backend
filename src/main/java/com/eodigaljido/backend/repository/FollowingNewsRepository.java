package com.eodigaljido.backend.repository;

import com.eodigaljido.backend.domain.following.FollowingNews;
import com.eodigaljido.backend.domain.following.FollowingNewsActionType;
import com.eodigaljido.backend.domain.friend.Friend;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FollowingNewsRepository extends JpaRepository<FollowingNews, Long> {

    @Query("""
            SELECT n FROM FollowingNews n
            JOIN FETCH n.actor actor
            WHERE (:type IS NULL OR n.actionType = :type)
              AND (:cursorId IS NULL OR n.id < :cursorId)
              AND EXISTS (
                    SELECT f FROM Friend f
                    WHERE f.status = :friendStatus
                      AND ((f.requester.id = :viewerId AND f.receiver.id = actor.id)
                        OR (f.receiver.id = :viewerId AND f.requester.id = actor.id))
                  )
            ORDER BY n.createdAt DESC, n.id DESC
            """)
    List<FollowingNews> findVisibleNews(@Param("viewerId") Long viewerId,
                                        @Param("friendStatus") Friend.FriendStatus friendStatus,
                                        @Param("type") FollowingNewsActionType type,
                                        @Param("cursorId") Long cursorId,
                                        Pageable pageable);

    @Query("""
            SELECT n FROM FollowingNews n
            JOIN FETCH n.actor actor
            WHERE n.id = :newsId
              AND EXISTS (
                    SELECT f FROM Friend f
                    WHERE f.status = :friendStatus
                      AND ((f.requester.id = :viewerId AND f.receiver.id = actor.id)
                        OR (f.receiver.id = :viewerId AND f.requester.id = actor.id))
                  )
            """)
    Optional<FollowingNews> findVisibleNewsById(@Param("viewerId") Long viewerId,
                                                @Param("friendStatus") Friend.FriendStatus friendStatus,
                                                @Param("newsId") Long newsId);

    @Query("""
            SELECT n FROM FollowingNews n
            JOIN FETCH n.actor actor
            WHERE EXISTS (
                    SELECT f FROM Friend f
                    WHERE f.status = :friendStatus
                      AND ((f.requester.id = :viewerId AND f.receiver.id = actor.id)
                        OR (f.receiver.id = :viewerId AND f.requester.id = actor.id))
                  )
              AND NOT EXISTS (
                    SELECT r FROM FollowingNewsRead r
                    WHERE r.news = n AND r.user.id = :viewerId
                  )
            """)
    List<FollowingNews> findUnreadVisibleNews(@Param("viewerId") Long viewerId,
                                              @Param("friendStatus") Friend.FriendStatus friendStatus);
}
