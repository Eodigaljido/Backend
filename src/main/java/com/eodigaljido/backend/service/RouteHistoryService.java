package com.eodigaljido.backend.service;

import com.eodigaljido.backend.domain.chat.ChatMessage;
import com.eodigaljido.backend.domain.chat.ChatRoom;
import com.eodigaljido.backend.domain.group.Group;
import com.eodigaljido.backend.domain.route.Route;
import com.eodigaljido.backend.domain.route.RouteEditLog;
import com.eodigaljido.backend.domain.user.Profile;
import com.eodigaljido.backend.domain.user.User;
import com.eodigaljido.backend.dto.course.PageInfo;
import com.eodigaljido.backend.dto.routehistory.RouteHistoryFeedItem;
import com.eodigaljido.backend.dto.routehistory.RouteHistoryFeedResponse;
import com.eodigaljido.backend.dto.routehistory.RouteHistoryItemResponse;
import com.eodigaljido.backend.exception.ChatException;
import com.eodigaljido.backend.exception.RouteException;
import com.eodigaljido.backend.exception.UserException;
import com.eodigaljido.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteHistoryService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final RouteRepository routeRepository;
    private final RouteEditLogRepository routeEditLogRepository;
    private final CourseMemberRepository courseMemberRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    // ──────────────────────────────────────────────────────────
    // 루트 기록 목록 (채팅방 기준)
    // ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RouteHistoryItemResponse> getRouteHistories(Long userId, String chatRoomUuid) {
        ChatRoom chatRoom = chatRoomRepository.findByUuidAndDeletedAtIsNull(chatRoomUuid)
                .orElseThrow(() -> new ChatException("채팅방을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        User user = findUser(userId);
        chatRoomMemberRepository.findByRoomAndUserAndLeftAtIsNull(chatRoom, user)
                .orElseThrow(() -> new ChatException("채팅방 멤버가 아닙니다.", HttpStatus.FORBIDDEN));

        Group group = chatRoom.getGroup();
        if (group == null) {
            return List.of();
        }

        List<Route> routes = routeRepository.findByGroupAndStatusNot(group, Route.RouteStatus.DELETED);

        return routes.stream()
                .filter(r -> r.getChatRoom() != null)
                .map(r -> {
                    long participantCount = courseMemberRepository.countByRoute(r);
                    return new RouteHistoryItemResponse(
                            r.getUuid(),
                            r.getChatRoom().getUuid(),
                            r.getTitle(),
                            participantCount
                    );
                })
                .toList();
    }

    // ──────────────────────────────────────────────────────────
    // 루트 기록 피드 (채팅 메시지 + 수정 이벤트 시간순)
    // ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public RouteHistoryFeedResponse getRouteHistoryFeed(Long userId, String courseId, int page, int size) {
        Route route = routeRepository.findByUuidAndStatusNot(courseId, Route.RouteStatus.DELETED)
                .orElseThrow(() -> new RouteException("루트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        User user = findUser(userId);
        verifyFeedAccess(route, user);

        List<ChatMessage> messages = route.getChatRoom() != null
                ? chatMessageRepository.findByRoomOrderByCreatedAtAsc(route.getChatRoom())
                : List.of();

        List<RouteEditLog> editLogs = routeEditLogRepository.findByRouteOrderByCreatedAtAsc(route);

        // 프로필 일괄 조회
        List<User> involvedUsers = new ArrayList<>();
        messages.forEach(m -> involvedUsers.add(m.getSender()));
        editLogs.forEach(e -> involvedUsers.add(e.getEditor()));

        Map<Long, Profile> profiles = profileRepository.findByUserIn(involvedUsers)
                .stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), Function.identity(), (a, b) -> a));

        List<RouteHistoryFeedItem> combined = new ArrayList<>();
        messages.stream()
                .map(m -> RouteHistoryFeedItem.fromChatMessage(m, profiles.get(m.getSender().getId())))
                .forEach(combined::add);
        editLogs.stream()
                .map(e -> RouteHistoryFeedItem.fromEditLog(e, profiles.get(e.getEditor().getId())))
                .forEach(combined::add);

        combined.sort(Comparator.comparing(RouteHistoryFeedItem::createdAt));

        int total = combined.size();
        int start = page * size;
        List<RouteHistoryFeedItem> pageItems = start >= total
                ? List.of()
                : combined.subList(start, Math.min(start + size, total));
        int totalPages = (int) Math.ceil((double) total / Math.max(size, 1));

        return new RouteHistoryFeedResponse(pageItems, new PageInfo(page, size, total, totalPages));
    }

    // ──────────────────────────────────────────────────────────
    // private helpers
    // ──────────────────────────────────────────────────────────

    private void verifyFeedAccess(Route route, User user) {
        // CourseMember이거나, 루트 전용 채팅방 멤버이거나, 그룹 채팅방 멤버이면 허용
        if (courseMemberRepository.existsByRouteAndUserAndLeftAtIsNull(route, user)) {
            return;
        }
        if (route.getChatRoom() != null
                && chatRoomMemberRepository.findByRoomAndUserAndLeftAtIsNull(route.getChatRoom(), user).isPresent()) {
            return;
        }
        if (route.getGroup() != null) {
            List<ChatRoom> groupRooms = chatRoomRepository.findByGroupAndDeletedAtIsNull(route.getGroup());
            boolean isMemberOfGroupRoom = groupRooms.stream().anyMatch(
                    r -> chatRoomMemberRepository.findByRoomAndUserAndLeftAtIsNull(r, user).isPresent()
            );
            if (isMemberOfGroupRoom) {
                return;
            }
        }
        throw new RouteException("루트 기록에 접근할 권한이 없습니다.", HttpStatus.FORBIDDEN);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }
}
