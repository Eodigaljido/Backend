package com.eodigaljido.backend.service;

import com.eodigaljido.backend.domain.chat.ChatMessage;
import com.eodigaljido.backend.domain.chat.ChatRoom;
import com.eodigaljido.backend.domain.chat.ChatRoomMember;
import com.eodigaljido.backend.domain.notification.NotificationType;
import com.eodigaljido.backend.domain.route.Route;
import com.eodigaljido.backend.domain.user.Profile;
import com.eodigaljido.backend.domain.user.User;
import com.eodigaljido.backend.dto.chat.ChatEventEnvelope;
import com.eodigaljido.backend.dto.chat.ChatMessageResponse;
import com.eodigaljido.backend.dto.chat.ChatRoomResponse;
import com.eodigaljido.backend.dto.chat.CreateChatRoomResponse;
import com.eodigaljido.backend.dto.chat.EditMessageRequest;
import com.eodigaljido.backend.dto.chat.InviteMemberRequest;
import com.eodigaljido.backend.dto.chat.SendMessageRequest;
import com.eodigaljido.backend.dto.chat.ShareRouteRequest;
import com.eodigaljido.backend.dto.chat.TypingEvent;
import com.eodigaljido.backend.dto.chat.TypingRequest;
import com.eodigaljido.backend.dto.chat.UpdateChatRoomNameRequest;
import com.eodigaljido.backend.dto.chat.ChatEventEnvelope.EventType;
import com.eodigaljido.backend.event.NotificationEvent;
import com.eodigaljido.backend.exception.ChatException;
import com.eodigaljido.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import org.springframework.data.domain.PageRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private static final List<String> DEFAULT_GROUP_IMAGES = List.of(
            "/images/chat/group-default-1.png",
            "/images/chat/group-default-2.png",
            "/images/chat/group-default-3.png",
            "/images/chat/group-default-4.png",
            "/images/chat/group-default-5.png"
    );
    private static final Random RANDOM = new Random();

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final RouteRepository routeRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final FileStorageService fileStorageService;

    @Transactional
    public CreateChatRoomResponse createRoom(Long userId, List<String> memberUuids, String name, MultipartFile image) {
        if (memberUuids == null || memberUuids.isEmpty()) {
            throw new ChatException("초대할 멤버를 1명 이상 입력해야 합니다.", HttpStatus.BAD_REQUEST);
        }

        User me = getUser(userId);

        List<User> invitees = memberUuids.stream()
                .distinct()
                .filter(uuid -> !uuid.equals(me.getUuid()))
                .map(uuid -> userRepository.findByUuid(uuid)
                        .orElseThrow(() -> new ChatException("존재하지 않는 유저입니다: " + uuid, HttpStatus.NOT_FOUND)))
                .collect(Collectors.toList());

        String roomName = (name != null && !name.isBlank()) ? name : null;

        // 초대 인원이 2명 이상이면 GROUP, 그 외 DIRECT
        ChatRoom.RoomType roomType = invitees.size() > 1 ? ChatRoom.RoomType.GROUP : ChatRoom.RoomType.DIRECT;

        String roomUuid = UUID.randomUUID().toString();
        String profileImageUrl = null;
        if (roomType == ChatRoom.RoomType.GROUP) {
            if (image != null && !image.isEmpty()) {
                try {
                    profileImageUrl = fileStorageService.store(image, "chat-rooms", roomUuid);
                } catch (IOException e) {
                    throw new ChatException("프로필 이미지 업로드 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
                }
            } else {
                profileImageUrl = randomDefaultImage();
            }
        }

        ChatRoom room = ChatRoom.builder()
                .uuid(roomUuid)
                .createdBy(me)
                .name(roomName)
                .type(roomType)
                .profileImageUrl(profileImageUrl)
                .build();
        chatRoomRepository.save(room);

        List<ChatRoomMember> members = new ArrayList<>();
        members.add(ChatRoomMember.builder().room(room).user(me).role(ChatRoomMember.MemberRole.ADMIN).build());
        for (User invitee : invitees) {
            members.add(ChatRoomMember.builder().room(room).user(invitee).role(ChatRoomMember.MemberRole.MEMBER).build());
        }
        chatRoomMemberRepository.saveAll(members);

        String resolvedName = room.getName() != null ? room.getName() : generateRoomName(room, members, userId);

        List<String> resolvedMemberUuids = members.stream().map(m -> m.getUser().getUuid()).toList();
        List<String> memberUserIds = members.stream().map(m -> m.getUser().getUserId()).toList();

        return new CreateChatRoomResponse(
                room.getUuid(),
                resolvedName,
                room.getProfileImageUrl(),
                members.size(),
                me.getUuid(),
                me.getUserId(),
                resolvedMemberUuids,
                memberUserIds
        );
    }

    @Transactional
    public ChatRoomResponse inviteMember(Long requesterId, String roomUuid, String targetUserId) {
        ChatRoom room = getActiveRoom(roomUuid);
        getMembership(room, requesterId);

        User target = userRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new ChatException("존재하지 않는 유저입니다: " + targetUserId, HttpStatus.NOT_FOUND));

        if (chatRoomMemberRepository.findByRoomAndUserAndLeftAtIsNull(room, target).isPresent()) {
            throw new ChatException("이미 채팅방에 참여 중인 유저입니다.", HttpStatus.CONFLICT);
        }

        chatRoomMemberRepository.save(
                ChatRoomMember.builder().room(room).user(target).role(ChatRoomMember.MemberRole.MEMBER).build()
        );

        User requester = getUser(requesterId);
        String requesterNickname = profileRepository.findByUser(requester)
                .map(Profile::getNickname).orElse(requester.getUserId());
        eventPublisher.publishEvent(NotificationEvent.of(
                target.getId(), requesterId,
                NotificationType.CHAT_ROOM_INVITED,
                "채팅방 초대",
                requesterNickname + "님이 채팅방에 초대했습니다.",
                room.getUuid(), "CHAT_ROOM"
        ));

        List<ChatRoomMember> members = chatRoomMemberRepository.findByRoomAndLeftAtIsNull(room);
        return buildRoomResponse(room, members, requesterId);
    }

    public List<ChatRoomResponse> getRooms(Long userId) {
        User me = getUser(userId);
        List<ChatRoom> rooms = chatRoomRepository.findRoomsForUser(me);
        if (rooms.isEmpty()) {
            return List.of();
        }

        // 모든 채팅방의 멤버를 한 번에 조회 (N+1 방지)
        List<ChatRoomMember> allMembers = chatRoomMemberRepository.findByRoomInAndLeftAtIsNull(rooms);
        Map<Long, List<ChatRoomMember>> membersByRoom = allMembers.stream()
                .collect(Collectors.groupingBy(m -> m.getRoom().getId()));

        return rooms.stream()
                .map(room -> {
                    List<ChatRoomMember> members = membersByRoom.getOrDefault(room.getId(), List.of());
                    return buildRoomResponse(room, members, userId);
                })
                .collect(Collectors.toList());
    }

    public ChatRoomResponse getRoom(Long userId, String roomUuid) {
        ChatRoom room = getActiveRoom(roomUuid);
        getMembership(room, userId);
        List<ChatRoomMember> members = chatRoomMemberRepository.findByRoomAndLeftAtIsNull(room);
        return buildRoomResponse(room, members, userId);
    }

    @Transactional
    public void deleteRoom(Long userId, String roomUuid) {
        ChatRoom room = getActiveRoom(roomUuid);
        ChatRoomMember membership = getMembership(room, userId);
        if (membership.getRole() != ChatRoomMember.MemberRole.ADMIN) {
            throw new ChatException("채팅방 삭제 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
        room.delete();
    }

    @Transactional
    public void leaveRoom(Long userId, String roomUuid) {
        ChatRoom room = getActiveRoom(roomUuid);
        ChatRoomMember membership = getMembership(room, userId);
        membership.leave();

        List<ChatRoomMember> remaining = chatRoomMemberRepository.findByRoomAndLeftAtIsNull(room);
        if (remaining.isEmpty()) {
            room.delete();
        } else if (membership.getRole() == ChatRoomMember.MemberRole.ADMIN) {
            remaining.get(0).promoteToAdmin();
        }
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long userId, String roomUuid, SendMessageRequest req) {
        ChatRoom room = getActiveRoom(roomUuid);
        ChatRoomMember senderMembership = getMembership(room, userId);
        User me = senderMembership.getUser();

        ChatMessage message = ChatMessage.builder()
                .uuid(UUID.randomUUID().toString())
                .room(room)
                .sender(me)
                .type(ChatMessage.MessageType.TEXT)
                .content(req.content())
                .build();
        chatMessageRepository.save(message);
        senderMembership.updateLastReadAt();

        String senderNickname = profileRepository.findByUser(me)
                .map(Profile::getNickname).orElse(me.getUserId());
        List<ChatRoomMember> otherMembers = chatRoomMemberRepository.findByRoomAndLeftAtIsNull(room)
                .stream().filter(m -> !m.getUser().getId().equals(userId)).toList();

        for (ChatRoomMember member : otherMembers) {
            Long recipientId = member.getUser().getId();
            eventPublisher.publishEvent(NotificationEvent.of(
                    recipientId, userId,
                    NotificationType.CHAT_MESSAGE,
                    senderNickname,
                    req.content().length() > 50 ? req.content().substring(0, 50) + "…" : req.content(),
                    room.getUuid(), "CHAT_ROOM"
            ));
        }

        if (req.mentionedUserUuids() != null) {
            for (String mentionedUuid : req.mentionedUserUuids()) {
                userRepository.findByUuid(mentionedUuid).ifPresent(mentionedUser -> {
                    if (!mentionedUser.getId().equals(userId)) {
                        eventPublisher.publishEvent(NotificationEvent.of(
                                mentionedUser.getId(), userId,
                                NotificationType.CHAT_MENTION,
                                senderNickname + "님이 멘션했습니다",
                                req.content().length() > 50 ? req.content().substring(0, 50) + "…" : req.content(),
                                room.getUuid(), "CHAT_ROOM"
                        ));
                    }
                });
            }
        }

        ChatMessageResponse response = toMessageResponse(message);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messagingTemplate.convertAndSend("/topic/chat/" + roomUuid,
                        new ChatEventEnvelope(EventType.MESSAGE_CREATED, response));
            }
        });
        return response;
    }

    @Transactional
    public ChatMessageResponse shareRoute(Long userId, String roomUuid, ShareRouteRequest req) {
        ChatRoom room = getActiveRoom(roomUuid);
        ChatRoomMember senderMembership = getMembership(room, userId);
        User me = senderMembership.getUser();

        Route route = routeRepository.findByUuidAndStatusNot(req.routeUuid(), Route.RouteStatus.DELETED)
                .orElseThrow(() -> new ChatException("존재하지 않는 루트입니다.", HttpStatus.NOT_FOUND));

        ChatMessage message = ChatMessage.builder()
                .uuid(UUID.randomUUID().toString())
                .room(room)
                .sender(me)
                .type(ChatMessage.MessageType.ROUTE)
                .content(route.getTitle())
                .refRoute(route)
                .build();
        chatMessageRepository.save(message);
        senderMembership.updateLastReadAt();

        String senderNickname = profileRepository.findByUser(me)
                .map(Profile::getNickname).orElse(me.getUserId());
        chatRoomMemberRepository.findByRoomAndLeftAtIsNull(room).stream()
                .filter(m -> !m.getUser().getId().equals(userId))
                .forEach(member -> eventPublisher.publishEvent(NotificationEvent.of(
                        member.getUser().getId(), userId,
                        NotificationType.CHAT_ROUTE_SHARED,
                        "루트 공유",
                        senderNickname + "님이 루트를 공유했습니다: " + route.getTitle(),
                        route.getUuid(), "ROUTE"
                )));

        ChatMessageResponse response = toMessageResponse(message);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messagingTemplate.convertAndSend("/topic/chat/" + roomUuid,
                        new ChatEventEnvelope(EventType.MESSAGE_CREATED, response));
            }
        });
        return response;
    }

    public List<ChatMessageResponse> getMessages(Long userId, String roomUuid, String beforeMessageUuid, int limit) {
        ChatRoom room = getActiveRoom(roomUuid);
        getMembership(room, userId);

        int clampedLimit = Math.min(Math.max(limit, 1), 100);
        List<ChatMessage> messages;
        if (beforeMessageUuid != null && !beforeMessageUuid.isBlank()) {
            ChatMessage cursor = getMessage(beforeMessageUuid);
            messages = chatMessageRepository.findByRoomBeforeId(room, cursor.getId(), PageRequest.of(0, clampedLimit));
        } else {
            messages = chatMessageRepository.findByRoomOrderByCreatedAtDesc(room, PageRequest.of(0, clampedLimit));
        }
        return messages.stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChatMessageResponse editMessage(Long userId, String roomUuid, String messageUuid, EditMessageRequest req) {
        ChatRoom room = getActiveRoom(roomUuid);
        getMembership(room, userId);
        ChatMessage message = getMessage(messageUuid);

        if (!message.getRoom().getId().equals(room.getId())) {
            throw new ChatException("해당 채팅방의 메시지가 아닙니다.", HttpStatus.BAD_REQUEST);
        }
        if (!message.getSender().getId().equals(userId)) {
            throw new ChatException("메시지 수정 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
        message.edit(req.content());
        ChatMessageResponse response = toMessageResponse(message);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messagingTemplate.convertAndSend("/topic/chat/" + roomUuid,
                        new ChatEventEnvelope(EventType.MESSAGE_EDITED, response));
            }
        });
        return response;
    }

    @Transactional
    public void deleteMessage(Long userId, String roomUuid, String messageUuid) {
        ChatRoom room = getActiveRoom(roomUuid);
        ChatRoomMember membership = getMembership(room, userId);
        ChatMessage message = getMessage(messageUuid);

        if (!message.getRoom().getId().equals(room.getId())) {
            throw new ChatException("해당 채팅방의 메시지가 아닙니다.", HttpStatus.BAD_REQUEST);
        }
        boolean isSender = message.getSender().getId().equals(userId);
        boolean isAdmin = membership.getRole() == ChatRoomMember.MemberRole.ADMIN;
        if (!isSender && !isAdmin) {
            throw new ChatException("메시지 삭제 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }

        ChatMessageResponse deleteResponse = toMessageResponse(message);
        chatMessageRepository.delete(message);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messagingTemplate.convertAndSend("/topic/chat/" + roomUuid,
                        new ChatEventEnvelope(EventType.MESSAGE_DELETED, deleteResponse));
            }
        });
    }

    @Transactional
    public ChatRoomResponse updateRoomName(Long userId, String roomUuid, UpdateChatRoomNameRequest req) {
        ChatRoom room = getActiveRoom(roomUuid);
        ChatRoomMember membership = getMembership(room, userId);
        if (membership.getRole() != ChatRoomMember.MemberRole.ADMIN) {
            throw new ChatException("채팅방 이름 변경 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
        String newName = (req.name() != null && !req.name().isBlank()) ? req.name() : null;
        room.updateName(newName);
        List<ChatRoomMember> members = chatRoomMemberRepository.findByRoomAndLeftAtIsNull(room);
        return buildRoomResponse(room, members, userId);
    }

    @Transactional
    public void kickMember(Long userId, String roomUuid, String targetUuid) {
        ChatRoom room = getActiveRoom(roomUuid);
        ChatRoomMember membership = getMembership(room, userId);
        if (membership.getRole() != ChatRoomMember.MemberRole.ADMIN) {
            throw new ChatException("강퇴 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }

        User target = userRepository.findByUuid(targetUuid)
                .orElseThrow(() -> new ChatException("존재하지 않는 유저입니다.", HttpStatus.NOT_FOUND));

        if (target.getId().equals(userId)) {
            throw new ChatException("자기 자신은 강퇴할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }

        ChatRoomMember targetMembership = chatRoomMemberRepository.findByRoomAndUserAndLeftAtIsNull(room, target)
                .orElseThrow(() -> new ChatException("해당 유저는 채팅방 멤버가 아닙니다.", HttpStatus.NOT_FOUND));

        targetMembership.leave();
    }

    @Transactional
    public ChatMessageResponse sendImageMessage(Long userId, String roomUuid, MultipartFile image) {
        ChatRoom room = getActiveRoom(roomUuid);
        ChatRoomMember senderMembership = getMembership(room, userId);
        User me = senderMembership.getUser();

        String messageUuid = UUID.randomUUID().toString();
        String attachmentUrl;
        try {
            attachmentUrl = fileStorageService.store(image, "chat-messages/" + roomUuid, messageUuid);
        } catch (IllegalArgumentException e) {
            throw new ChatException(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            throw new ChatException("이미지 업로드 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        ChatMessage message = ChatMessage.builder()
                .uuid(messageUuid)
                .room(room)
                .sender(me)
                .type(ChatMessage.MessageType.IMAGE)
                .attachmentUrl(attachmentUrl)
                .build();
        chatMessageRepository.save(message);
        senderMembership.updateLastReadAt();

        String senderNickname = profileRepository.findByUser(me)
                .map(Profile::getNickname).orElse(me.getUserId());
        chatRoomMemberRepository.findByRoomAndLeftAtIsNull(room).stream()
                .filter(m -> !m.getUser().getId().equals(userId))
                .forEach(member -> eventPublisher.publishEvent(NotificationEvent.of(
                        member.getUser().getId(), userId,
                        NotificationType.CHAT_MESSAGE,
                        senderNickname,
                        "[이미지]",
                        room.getUuid(), "CHAT_ROOM"
                )));

        ChatMessageResponse response = toMessageResponse(message);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messagingTemplate.convertAndSend("/topic/chat/" + roomUuid,
                        new ChatEventEnvelope(EventType.MESSAGE_CREATED, response));
            }
        });
        return response;
    }

    public void broadcastTyping(Long userId, String roomUuid, TypingRequest req) {
        ChatRoom room = getActiveRoom(roomUuid);
        getMembership(room, userId);
        User me = getUser(userId);
        String nickname = profileRepository.findByUser(me)
                .map(Profile::getNickname).orElse(me.getUserId());
        messagingTemplate.convertAndSend(
                "/topic/chat/" + roomUuid + "/typing",
                new TypingEvent(me.getUuid(), nickname, req.isTyping())
        );
    }

    @Transactional
    public void markAsRead(Long userId, String roomUuid) {
        ChatRoom room = getActiveRoom(roomUuid);
        ChatRoomMember membership = getMembership(room, userId);
        membership.updateLastReadAt();
    }

    @Transactional
    public ChatRoomResponse updateRoomProfileImage(Long userId, String roomUuid, MultipartFile image) {
        ChatRoom room = getActiveRoom(roomUuid);
        ChatRoomMember membership = getMembership(room, userId);
        if (membership.getRole() != ChatRoomMember.MemberRole.ADMIN) {
            throw new ChatException("프로필 이미지 변경 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
        if (room.getType() != ChatRoom.RoomType.GROUP) {
            throw new ChatException("그룹 채팅방에서만 프로필 이미지를 설정할 수 있습니다.", HttpStatus.BAD_REQUEST);
        }

        String oldUrl = room.getProfileImageUrl();
        String newUrl;
        try {
            newUrl = fileStorageService.store(image, "chat-rooms", room.getUuid());
        } catch (IOException e) {
            throw new ChatException("파일 업로드 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        if (oldUrl != null && oldUrl.startsWith("/uploads/")) {
            fileStorageService.delete(oldUrl);
        }
        room.updateProfileImage(newUrl);

        List<ChatRoomMember> members = chatRoomMemberRepository.findByRoomAndLeftAtIsNull(room);
        return buildRoomResponse(room, members, userId);
    }

    @Transactional
    public ChatRoomResponse resetRoomProfileImage(Long userId, String roomUuid) {
        ChatRoom room = getActiveRoom(roomUuid);
        ChatRoomMember membership = getMembership(room, userId);
        if (membership.getRole() != ChatRoomMember.MemberRole.ADMIN) {
            throw new ChatException("프로필 이미지 변경 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
        if (room.getType() != ChatRoom.RoomType.GROUP) {
            throw new ChatException("그룹 채팅방에서만 프로필 이미지를 설정할 수 있습니다.", HttpStatus.BAD_REQUEST);
        }

        String oldUrl = room.getProfileImageUrl();
        if (oldUrl != null && oldUrl.startsWith("/uploads/")) {
            fileStorageService.delete(oldUrl);
        }
        room.updateProfileImage(randomDefaultImage());

        List<ChatRoomMember> members = chatRoomMemberRepository.findByRoomAndLeftAtIsNull(room);
        return buildRoomResponse(room, members, userId);
    }

    // ── helpers ──────────────────────────────────────────────

    private ChatRoomResponse buildRoomResponse(ChatRoom room, List<ChatRoomMember> members, Long currentUserId) {
        String name = room.getName() != null ? room.getName() : generateRoomName(room, members, currentUserId);

        User owner = room.getCreatedBy();
        List<String> memberUuids = members.stream()
                .sorted(Comparator.comparingLong(ChatRoomMember::getId))
                .limit(3)
                .map(m -> m.getUser().getUuid())
                .toList();
        List<String> memberUserIds = members.stream()
                .sorted(Comparator.comparingLong(ChatRoomMember::getId))
                .limit(3)
                .map(m -> m.getUser().getUserId())
                .toList();

        ChatMessage lastMsg = chatMessageRepository.findTopByRoomOrderByCreatedAtDesc(room).orElse(null);
        String lastContent = lastMsg != null ? lastMsg.getContent() : null;
        java.time.LocalDateTime lastMsgAt = lastMsg != null ? lastMsg.getCreatedAt() : null;

        long unreadCount = members.stream()
                .filter(m -> m.getUser().getId().equals(currentUserId))
                .findFirst()
                .map(m -> {
                    if (m.getLastReadAt() == null) {
                        return chatMessageRepository.countActiveByRoom(room);
                    }
                    return chatMessageRepository.countMessagesAfter(room, m.getLastReadAt());
                })
                .orElse(0L);

        String profileImageUrl;
        if (room.getType() == ChatRoom.RoomType.DIRECT) {
            profileImageUrl = members.stream()
                    .filter(m -> !m.getUser().getId().equals(currentUserId))
                    .findFirst()
                    .flatMap(m -> profileRepository.findByUser(m.getUser()))
                    .map(Profile::getProfileImageUrl)
                    .orElse(null);
        } else {
            profileImageUrl = room.getProfileImageUrl();
        }

        return new ChatRoomResponse(
                room.getUuid(), name, profileImageUrl, members.size(),
                owner.getUuid(), owner.getUserId(),
                memberUuids, memberUserIds,
                lastContent, lastMsgAt, unreadCount
        );
    }

    private String generateRoomName(ChatRoom room, List<ChatRoomMember> members, Long currentUserId) {
        Long ownerId = room.getCreatedBy().getId();
        String names = members.stream()
                .filter(m -> !m.getUser().getId().equals(ownerId) && !m.getUser().getId().equals(currentUserId))
                .map(m -> profileRepository.findByUser(m.getUser())
                        .map(Profile::getNickname)
                        .orElse("알 수 없음"))
                .collect(Collectors.joining(", "));
        return names.isBlank() ? "나와의 채팅" : names + "의 채팅방";
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        Profile senderProfile = profileRepository.findByUser(message.getSender()).orElse(null);
        String nickname = senderProfile != null ? senderProfile.getNickname() : "알 수 없음";
        String imageUrl = senderProfile != null ? senderProfile.getProfileImageUrl() : null;

        Route route = message.getRefRoute();
        String routeUuid = route != null ? route.getUuid() : null;
        String routeTitle = route != null ? route.getTitle() : null;
        String routeThumbnailUrl = route != null ? route.getThumbnailUrl() : null;

        return new ChatMessageResponse(
                message.getUuid(),
                message.getSender().getUuid(),
                nickname,
                imageUrl,
                message.getType().name(),
                message.getContent(),
                message.getAttachmentUrl(),
                routeUuid,
                routeTitle,
                routeThumbnailUrl,
                message.getCreatedAt(),
                message.getEditedAt()
        );
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ChatException("유저를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    private ChatRoom getActiveRoom(String roomUuid) {
        return chatRoomRepository.findByUuidAndDeletedAtIsNull(roomUuid)
                .orElseThrow(() -> new ChatException("채팅방을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    private ChatRoomMember getMembership(ChatRoom room, Long userId) {
        User user = getUser(userId);
        return chatRoomMemberRepository.findByRoomAndUserAndLeftAtIsNull(room, user)
                .orElseThrow(() -> new ChatException("채팅방 멤버가 아닙니다.", HttpStatus.FORBIDDEN));
    }

    private ChatMessage getMessage(String messageUuid) {
        return chatMessageRepository.findByUuid(messageUuid)
                .orElseThrow(() -> new ChatException("메시지를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    private String randomDefaultImage() {
        return DEFAULT_GROUP_IMAGES.get(RANDOM.nextInt(DEFAULT_GROUP_IMAGES.size()));
    }
}
