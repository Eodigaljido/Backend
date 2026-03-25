package com.eodigaljido.backend.service;

import com.eodigaljido.backend.domain.chat.ChatMessage;
import com.eodigaljido.backend.domain.chat.ChatRoom;
import com.eodigaljido.backend.domain.chat.ChatRoomMember;
import com.eodigaljido.backend.domain.user.Profile;
import com.eodigaljido.backend.domain.user.User;
import com.eodigaljido.backend.dto.chat.*;
import com.eodigaljido.backend.exception.ChatException;
import com.eodigaljido.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ChatRoomResponse createRoom(Long userId, CreateChatRoomRequest req) {
        User me = getUser(userId);

        List<User> invitees = req.memberUuids().stream()
                .filter(uuid -> !uuid.equals(me.getUuid()))
                .map(uuid -> userRepository.findByUuid(uuid)
                        .orElseThrow(() -> new ChatException("존재하지 않는 유저입니다: " + uuid, HttpStatus.NOT_FOUND)))
                .collect(Collectors.toList());

        String roomName = (req.name() != null && !req.name().isBlank()) ? req.name() : null;
        ChatRoom room = ChatRoom.builder()
                .uuid(UUID.randomUUID().toString())
                .createdBy(me)
                .name(roomName)
                .build();
        chatRoomRepository.save(room);

        List<ChatRoomMember> members = new ArrayList<>();
        members.add(ChatRoomMember.builder().room(room).user(me).role(ChatRoomMember.MemberRole.ADMIN).build());
        for (User invitee : invitees) {
            members.add(ChatRoomMember.builder().room(room).user(invitee).role(ChatRoomMember.MemberRole.MEMBER).build());
        }
        chatRoomMemberRepository.saveAll(members);

        return buildRoomResponse(room, members, userId);
    }

    public List<ChatRoomResponse> getRooms(Long userId) {
        User me = getUser(userId);
        List<ChatRoom> rooms = chatRoomRepository.findRoomsForUser(me);
        return rooms.stream()
                .map(room -> {
                    List<ChatRoomMember> members = chatRoomMemberRepository.findByRoomAndLeftAtIsNull(room);
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

        long remaining = chatRoomMemberRepository.findByRoomAndLeftAtIsNull(room).size();
        if (remaining == 0) {
            room.delete();
        }
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long userId, String roomUuid, SendMessageRequest req) {
        ChatRoom room = getActiveRoom(roomUuid);
        User me = getMembership(room, userId).getUser();

        ChatMessage message = ChatMessage.builder()
                .uuid(UUID.randomUUID().toString())
                .room(room)
                .sender(me)
                .type(ChatMessage.MessageType.TEXT)
                .content(req.content())
                .build();
        chatMessageRepository.save(message);

        ChatMessageResponse response = toMessageResponse(message);
        messagingTemplate.convertAndSend("/topic/chat/" + roomUuid, response);
        return response;
    }

    public List<ChatMessageResponse> getMessages(Long userId, String roomUuid) {
        ChatRoom room = getActiveRoom(roomUuid);
        getMembership(room, userId);

        List<ChatMessage> messages = chatMessageRepository.findTop50ByRoomOrderByCreatedAtDesc(room);
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
        if (message.isDeleted()) {
            throw new ChatException("삭제된 메시지는 수정할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }

        message.edit(req.content());
        return toMessageResponse(message);
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

        message.delete();
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
    public void markAsRead(Long userId, String roomUuid) {
        ChatRoom room = getActiveRoom(roomUuid);
        ChatRoomMember membership = getMembership(room, userId);
        membership.updateLastReadAt();
    }

    // ── helpers ──────────────────────────────────────────────

    private ChatRoomResponse buildRoomResponse(ChatRoom room, List<ChatRoomMember> members, Long currentUserId) {
        String name = room.getName() != null ? room.getName() : generateRoomName(members, currentUserId);

        ChatMessage lastMsg = chatMessageRepository.findTopByRoomOrderByCreatedAtDesc(room).orElse(null);
        String lastContent = lastMsg != null && !lastMsg.isDeleted() ? lastMsg.getContent() : null;
        java.time.LocalDateTime lastMsgAt = lastMsg != null ? lastMsg.getCreatedAt() : null;

        long unreadCount = members.stream()
                .filter(m -> m.getUser().getId().equals(currentUserId))
                .findFirst()
                .map(m -> {
                    if (m.getLastReadAt() == null) {
                        return chatMessageRepository.countActiveByRoom(room);
                    }
                    return chatRoomMemberRepository.countMessagesAfter(room, m.getLastReadAt());
                })
                .orElse(0L);

        return new ChatRoomResponse(room.getUuid(), name, members.size(), lastContent, lastMsgAt, unreadCount);
    }

    private String generateRoomName(List<ChatRoomMember> members, Long currentUserId) {
        String names = members.stream()
                .filter(m -> !m.getUser().getId().equals(currentUserId))
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
        String content = message.isDeleted() ? null : message.getContent();

        return new ChatMessageResponse(
                message.getUuid(),
                message.getSender().getUuid(),
                nickname,
                imageUrl,
                content,
                message.getCreatedAt(),
                message.getEditedAt(),
                message.isDeleted()
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
}
