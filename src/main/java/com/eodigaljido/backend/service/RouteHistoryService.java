package com.eodigaljido.backend.service;

import com.eodigaljido.backend.domain.chat.ChatMessage;
import com.eodigaljido.backend.domain.chat.ChatRoom;
import com.eodigaljido.backend.domain.route.Route;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RouteHistoryService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final RouteRepository routeRepository;
    private final CourseMemberRepository courseMemberRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    // ──────────────────────────────────────────────────────────
    // 루트 기록 목록 (채팅방 기준)
    // ──────────────────────────────────────────────────────────

    public List<RouteHistoryItemResponse> getRouteHistories(Long userId, String chatRoomUuid) {
        ChatRoom chatRoom = chatRoomRepository.findByUuidAndDeletedAtIsNull(chatRoomUuid)
                .orElseThrow(() -> new ChatException("채팅방을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        User user = findUser(userId);
        chatRoomMemberRepository.findByRoomAndUserAndLeftAtIsNull(chatRoom, user)
                .orElseThrow(() -> new ChatException("채팅방 멤버가 아닙니다.", HttpStatus.FORBIDDEN));

        List<ChatRoom> childRooms = chatRoomRepository.findByParentRoomAndDeletedAtIsNull(chatRoom);
        if (childRooms.isEmpty()) {
            return List.of();
        }

        return childRooms.stream()
                .map(room -> routeRepository.findByChatRoomIdAndStatusNot(room.getId(), Route.RouteStatus.DELETED)
                        .map(route -> new RouteHistoryItemResponse(
                                route.getUuid(),
                                room.getUuid(),
                                route.getTitle(),
                                courseMemberRepository.countByRoute(route)
                        ))
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    // ──────────────────────────────────────────────────────────
    // 루트 기록 상세 피드 (루트에 연결된 채팅방의 chat_messages 기반)
    // ──────────────────────────────────────────────────────────

    public RouteHistoryFeedResponse getRouteHistoryFeed(Long userId, String courseId, int page, int size) {
        Route route = routeRepository.findByUuidAndStatusNot(courseId, Route.RouteStatus.DELETED)
                .orElseThrow(() -> new RouteException("루트를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        User user = findUser(userId);
        verifyFeedAccess(route, user);

        if (route.getChatRoom() == null) {
            return new RouteHistoryFeedResponse(List.of(), new PageInfo(page, size, 0, 0));
        }

        Page<ChatMessage> messagePage = chatMessageRepository.findByRouteOrderByCreatedAtAsc(
                route, PageRequest.of(page, size));

        List<User> senders = messagePage.getContent().stream().map(ChatMessage::getSender).distinct().toList();
        Map<Long, Profile> profiles = profileRepository.findByUserIn(senders)
                .stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), Function.identity(), (a, b) -> a));

        List<RouteHistoryFeedItem> items = messagePage.getContent().stream()
                .map(message -> RouteHistoryFeedItem.from(message, profiles.get(message.getSender().getId())))
                .toList();

        PageInfo pageInfo = new PageInfo(messagePage.getNumber(), messagePage.getSize(),
                messagePage.getTotalElements(), messagePage.getTotalPages());
        return new RouteHistoryFeedResponse(items, pageInfo);
    }

    // ──────────────────────────────────────────────────────────
    // private helpers
    // ──────────────────────────────────────────────────────────

    private void verifyFeedAccess(Route route, User user) {
        if (courseMemberRepository.existsByRouteAndUserAndLeftAtIsNull(route, user)) {
            return;
        }
        if (route.getChatRoom() != null) {
            if (chatRoomMemberRepository.findByRoomAndUserAndLeftAtIsNull(route.getChatRoom(), user).isPresent()) {
                return;
            }
            ChatRoom parentRoom = route.getChatRoom().getParentRoom();
            if (parentRoom != null
                    && chatRoomMemberRepository.findByRoomAndUserAndLeftAtIsNull(parentRoom, user).isPresent()) {
                return;
            }
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
