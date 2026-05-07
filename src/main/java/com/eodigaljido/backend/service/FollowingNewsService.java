package com.eodigaljido.backend.service;

import com.eodigaljido.backend.domain.following.FollowingNews;
import com.eodigaljido.backend.domain.following.FollowingNewsActionType;
import com.eodigaljido.backend.domain.following.FollowingNewsRead;
import com.eodigaljido.backend.domain.friend.Friend;
import com.eodigaljido.backend.domain.user.Profile;
import com.eodigaljido.backend.domain.user.User;
import com.eodigaljido.backend.dto.following.FollowingNewsFeedResponse;
import com.eodigaljido.backend.dto.following.FollowingNewsItemResponse;
import com.eodigaljido.backend.exception.FollowingNewsException;
import com.eodigaljido.backend.repository.FollowingNewsReadRepository;
import com.eodigaljido.backend.repository.FollowingNewsRepository;
import com.eodigaljido.backend.repository.ProfileRepository;
import com.eodigaljido.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowingNewsService {

    private static final int DEFAULT_LIMIT = 3;
    private static final int MAX_LIMIT = 50;

    private final FollowingNewsRepository followingNewsRepository;
    private final FollowingNewsReadRepository followingNewsReadRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    @Transactional(readOnly = true)
    public FollowingNewsFeedResponse getNews(Long userId, int limit, String cursor, FollowingNewsActionType type) {
        int normalizedLimit = normalizeLimit(limit);
        Long cursorId = decodeNewsId(cursor, "cursor");

        List<FollowingNews> fetched = followingNewsRepository.findVisibleNews(
                userId,
                Friend.FriendStatus.ACCEPTED,
                type,
                cursorId,
                PageRequest.of(0, normalizedLimit + 1)
        );

        boolean hasNext = fetched.size() > normalizedLimit;
        List<FollowingNews> visibleItems = hasNext ? fetched.subList(0, normalizedLimit) : fetched;
        Map<Long, Profile> profilesByUserId = findProfilesByUserId(visibleItems);

        List<FollowingNewsItemResponse> items = visibleItems.stream()
                .map(news -> FollowingNewsItemResponse.from(
                        news,
                        resolveNickname(news.getActor(), profilesByUserId.get(news.getActor().getId())),
                        formatTimeAgo(news.getCreatedAt())
                ))
                .toList();

        String nextCursor = hasNext && !visibleItems.isEmpty()
                ? FollowingNewsItemResponse.encodeId(visibleItems.get(visibleItems.size() - 1).getId())
                : null;

        return new FollowingNewsFeedResponse(items, nextCursor);
    }

    @Transactional
    public void markAsRead(Long userId, String newsId) {
        User user = findUser(userId);
        Long decodedNewsId = decodeNewsId(newsId, "id");
        FollowingNews news = followingNewsRepository.findVisibleNewsById(
                        userId, Friend.FriendStatus.ACCEPTED, decodedNewsId)
                .orElseThrow(() -> new FollowingNewsException("소식을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        if (!followingNewsReadRepository.existsByUserAndNews(user, news)) {
            followingNewsReadRepository.save(FollowingNewsRead.builder()
                    .user(user)
                    .news(news)
                    .build());
        }
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        User user = findUser(userId);
        List<FollowingNewsRead> reads = followingNewsRepository
                .findUnreadVisibleNews(userId, Friend.FriendStatus.ACCEPTED)
                .stream()
                .map(news -> FollowingNewsRead.builder()
                        .user(user)
                        .news(news)
                        .build())
                .toList();

        followingNewsReadRepository.saveAll(reads);
    }

    @Transactional
    public void createNews(Long actorUserId, FollowingNewsActionType actionType, String courseId, String courseName) {
        User actor = findUser(actorUserId);
        followingNewsRepository.save(FollowingNews.builder()
                .actor(actor)
                .actionType(actionType)
                .courseId(courseId)
                .courseName(courseName)
                .build());
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new FollowingNewsException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private Long decodeNewsId(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String rawId = value.startsWith("news_") ? value.substring("news_".length()) : value;
        try {
            long id = Long.parseLong(rawId);
            if (id <= 0) {
                throw new NumberFormatException("non-positive id");
            }
            return id;
        } catch (NumberFormatException e) {
            throw new FollowingNewsException(fieldName + " 값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private Map<Long, Profile> findProfilesByUserId(List<FollowingNews> newsItems) {
        List<User> actors = newsItems.stream()
                .map(FollowingNews::getActor)
                .distinct()
                .toList();
        if (actors.isEmpty()) {
            return Map.of();
        }
        return profileRepository.findByUserIn(actors).stream()
                .collect(Collectors.toMap(
                        profile -> profile.getUser().getId(),
                        Function.identity(),
                        (left, right) -> left
                ));
    }

    private String resolveNickname(User user, Profile profile) {
        if (profile != null && profile.getNickname() != null && !profile.getNickname().isBlank()) {
            return profile.getNickname();
        }
        return user.getUserId() != null ? user.getUserId() : user.getUuid();
    }

    private String formatTimeAgo(LocalDateTime createdAt) {
        Duration duration = Duration.between(createdAt, LocalDateTime.now());
        if (duration.isNegative() || duration.toMinutes() < 1) {
            return "방금 전";
        }
        long minutes = duration.toMinutes();
        if (minutes < 60) {
            return minutes + "분 전";
        }
        long hours = duration.toHours();
        if (hours < 24) {
            return hours + "시간 전";
        }
        long days = duration.toDays();
        if (days < 30) {
            return days + "일 전";
        }
        long months = days / 30;
        if (months < 12) {
            return months + "개월 전";
        }
        return (days / 365) + "년 전";
    }
}
