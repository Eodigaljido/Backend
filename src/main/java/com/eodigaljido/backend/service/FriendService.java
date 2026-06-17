package com.eodigaljido.backend.service;

import com.eodigaljido.backend.domain.friend.Friend;
import com.eodigaljido.backend.domain.notification.NotificationType;
import com.eodigaljido.backend.domain.user.Profile;
import com.eodigaljido.backend.domain.user.User;
import com.eodigaljido.backend.dto.friend.FriendCodeResponse;
import com.eodigaljido.backend.dto.friend.FriendPreviewResponse;
import com.eodigaljido.backend.dto.friend.FriendRequestResponse;
import com.eodigaljido.backend.dto.friend.FriendResponse;
import com.eodigaljido.backend.dto.friend.RecentFriendResponse;
import com.eodigaljido.backend.exception.ChatException;
import com.eodigaljido.backend.exception.FriendException;
import com.eodigaljido.backend.exception.UserException;
import com.eodigaljido.backend.event.NotificationEvent;
import com.eodigaljido.backend.repository.ChatMessageRepository;
import com.eodigaljido.backend.repository.ChatRoomMemberRepository;
import com.eodigaljido.backend.repository.ChatRoomRepository;
import com.eodigaljido.backend.repository.FriendRepository;
import com.eodigaljido.backend.repository.ProfileRepository;
import com.eodigaljido.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendRepository friendRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final FriendCodeService friendCodeService;
    private final ApplicationEventPublisher eventPublisher;

    // 친구 코드로 초대자 preview 조회 (비로그인 허용)
    @Transactional(readOnly = true)
    public FriendPreviewResponse getFriendPreview(String friendCode) {
        User user = userRepository.findByFriendCode(friendCode.toUpperCase(Locale.ROOT))
                .filter(u -> u.getStatus() == com.eodigaljido.backend.domain.user.User.UserStatus.ACTIVE)
                .orElseThrow(() -> new FriendException("존재하지 않는 친구 코드입니다.", HttpStatus.NOT_FOUND));

        Profile profile = profileRepository.findByUser(user).orElse(null);
        return FriendPreviewResponse.of(user, profile);
    }

    // 친구 코드로 직접 친구 요청 전송 (POST /api/friends/add 전용)
    @Transactional
    public void addFriendByCode(Long requesterId, String friendCode) {
        User requester = findUser(requesterId);
        User target = userRepository.findByFriendCode(friendCode.toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new FriendException("존재하지 않는 친구 코드입니다.", HttpStatus.NOT_FOUND));

        if (requester.getId().equals(target.getId())) {
            throw new FriendException("자신의 친구 코드는 추가할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }

        friendRepository.findBetween(requester, target).ifPresent(f -> {
            switch (f.getStatus()) {
                case PENDING -> throw new FriendException("이미 대기 중인 친구 요청이 있습니다.", HttpStatus.CONFLICT);
                case ACCEPTED -> throw new FriendException("이미 친구입니다.", HttpStatus.CONFLICT);
                case REJECTED, BLOCKED -> throw new FriendException("친구 요청을 보낼 수 없는 상태입니다.", HttpStatus.CONFLICT);
            }
        });

        friendRepository.save(Friend.builder().requester(requester).receiver(target).build());
        publishFriendRequest(requester, target);
    }

    @Transactional
    public FriendCodeResponse getMyFriendCode(Long userId) {
        User me = findUser(userId);
        return new FriendCodeResponse(friendCodeService.assignIfMissing(me));
    }

    // 친구 목록 조회 (한글 → 영어 → 숫자 → 특수기호 순, 각 그룹 내 가나다/abc/123 정렬)
    @Transactional(readOnly = true)
    public List<FriendResponse> getFriends(Long userId) {
        User me = findUser(userId);
        List<Friend> friends = friendRepository.findAcceptedFriends(me);

        List<User> others = friends.stream()
                .map(f -> f.getRequester().getId().equals(me.getId()) ? f.getReceiver() : f.getRequester())
                .toList();

        Map<Long, Profile> profileMap = buildProfileMap(others);
        Collator koreanCollator = Collator.getInstance(Locale.KOREAN);

        return friends.stream()
                .map(f -> {
                    User other = f.getRequester().getId().equals(me.getId()) ? f.getReceiver() : f.getRequester();
                    return FriendResponse.of(f, me, profileMap.get(other.getId()));
                })
                .sorted((a, b) -> {
                    String na = a.nickname() != null ? a.nickname() : "";
                    String nb = b.nickname() != null ? b.nickname() : "";
                    if (na.isEmpty() && nb.isEmpty()) return 0;
                    if (na.isEmpty()) return 1;
                    if (nb.isEmpty()) return -1;
                    int ga = charGroup(na.charAt(0));
                    int gb = charGroup(nb.charAt(0));
                    if (ga != gb) return Integer.compare(ga, gb);
                    if (ga == 0) return koreanCollator.compare(na, nb);
                    return na.compareToIgnoreCase(nb);
                })
                .toList();
    }

    // 채팅방에 초대 가능한 친구 목록 (해당 채팅방 미참여 친구만)
    @Transactional(readOnly = true)
    public List<FriendResponse> getInvitableFriends(Long userId, String roomUuid) {
        User me = findUser(userId);

        com.eodigaljido.backend.domain.chat.ChatRoom room = chatRoomRepository.findByUuidAndDeletedAtIsNull(roomUuid)
                .orElseThrow(() -> new ChatException("존재하지 않는 채팅방입니다.", HttpStatus.NOT_FOUND));

        chatRoomMemberRepository.findByRoomAndUserAndLeftAtIsNull(room, me)
                .orElseThrow(() -> new ChatException("채팅방 멤버가 아닙니다.", HttpStatus.FORBIDDEN));

        Set<Long> memberUserIds = chatRoomMemberRepository.findByRoomAndLeftAtIsNull(room).stream()
                .map(m -> m.getUser().getId())
                .collect(Collectors.toSet());

        List<Friend> friends = friendRepository.findAcceptedFriends(me);

        List<Friend> invitableFriends = friends.stream()
                .filter(f -> {
                    User other = f.getRequester().getId().equals(me.getId()) ? f.getReceiver() : f.getRequester();
                    return !memberUserIds.contains(other.getId());
                })
                .toList();

        List<User> others = invitableFriends.stream()
                .map(f -> f.getRequester().getId().equals(me.getId()) ? f.getReceiver() : f.getRequester())
                .toList();

        Map<Long, Profile> profileMap = buildProfileMap(others);
        Collator koreanCollator = Collator.getInstance(Locale.KOREAN);

        return invitableFriends.stream()
                .map(f -> FriendResponse.of(f, me, profileMap.get(
                        (f.getRequester().getId().equals(me.getId()) ? f.getReceiver() : f.getRequester()).getId()
                )))
                .sorted((a, b) -> {
                    String na = a.nickname() != null ? a.nickname() : "";
                    String nb = b.nickname() != null ? b.nickname() : "";
                    if (na.isEmpty() && nb.isEmpty()) return 0;
                    if (na.isEmpty()) return 1;
                    if (nb.isEmpty()) return -1;
                    int ga = charGroup(na.charAt(0));
                    int gb = charGroup(nb.charAt(0));
                    if (ga != gb) return Integer.compare(ga, gb);
                    if (ga == 0) return koreanCollator.compare(na, nb);
                    return na.compareToIgnoreCase(nb);
                })
                .toList();
    }

    // 최근 연락한 친구 5명 조회 (마지막 메시지 시각 기준 내림차순)
    @Transactional(readOnly = true)
    public List<RecentFriendResponse> getRecentFriends(Long userId) {
        User me = findUser(userId);
        List<Friend> friends = friendRepository.findAcceptedFriends(me);
        if (friends.isEmpty()) return List.of();

        List<Long> friendIds = friends.stream()
                .map(f -> f.getRequester().getId().equals(me.getId()) ? f.getReceiver().getId() : f.getRequester().getId())
                .toList();

        List<Object[]> rows = chatMessageRepository.findRecentContactedFriends(userId, friendIds);
        if (rows.isEmpty()) return List.of();

        Map<Long, Friend> friendByUserId = friends.stream()
                .collect(Collectors.toMap(
                        f -> f.getRequester().getId().equals(me.getId()) ? f.getReceiver().getId() : f.getRequester().getId(),
                        f -> f
                ));

        List<User> recentUsers = rows.stream()
                .limit(5)
                .map(row -> {
                    Long fUserId = (Long) row[0];
                    Friend f = friendByUserId.get(fUserId);
                    return f.getRequester().getId().equals(me.getId()) ? f.getReceiver() : f.getRequester();
                })
                .toList();

        Map<Long, Profile> profileMap = buildProfileMap(recentUsers);

        return rows.stream()
                .limit(5)
                .map(row -> {
                    Long fUserId = (Long) row[0];
                    LocalDateTime lastContactedAt = (LocalDateTime) row[1];
                    Friend f = friendByUserId.get(fUserId);
                    User other = f.getRequester().getId().equals(me.getId()) ? f.getReceiver() : f.getRequester();
                    return RecentFriendResponse.of(f, me, profileMap.get(other.getId()), lastContactedAt);
                })
                .toList();
    }

    // 친구 요청 전송
    @Transactional
    public void sendRequest(Long requesterId, String targetUuid, String friendCode) {
        User requester = findUser(requesterId);
        User target = findTargetUser(targetUuid, friendCode)
                .orElseThrow(() -> new UserException("존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND));

        if (requester.getId().equals(target.getId())) {
            throw new FriendException("자기 자신에게 친구 요청을 보낼 수 없습니다.", HttpStatus.BAD_REQUEST);
        }

        friendRepository.findBetween(requester, target).ifPresent(f -> {
            switch (f.getStatus()) {
                case PENDING -> throw new FriendException("이미 대기 중인 친구 요청이 있습니다.", HttpStatus.CONFLICT);
                case ACCEPTED -> throw new FriendException("이미 친구 관계입니다.", HttpStatus.CONFLICT);
                case REJECTED, BLOCKED -> throw new FriendException("친구 요청을 보낼 수 없는 상태입니다.", HttpStatus.CONFLICT);
            }
        });

        Friend friend = Friend.builder()
                .requester(requester)
                .receiver(target)
                .build();
        friendRepository.save(friend);
        publishFriendRequest(requester, target);
    }

    // 친구 삭제
    @Transactional
    public void deleteFriend(Long userId, Long friendId) {
        User me = findUser(userId);
        Friend friend = friendRepository.findAcceptedById(friendId, me)
                .orElseThrow(() -> new FriendException("친구 관계를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        friendRepository.delete(friend);
    }

    // 친구 요청 수락/거절
    @Transactional
    public void respondToRequest(Long receiverId, Long requestId, boolean accept) {
        User receiver = findUser(receiverId);
        Friend request = friendRepository.findById(requestId)
                .orElseThrow(() -> new FriendException("친구 요청을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        if (!request.getReceiver().getId().equals(receiver.getId())) {
            throw new FriendException("해당 요청에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
        if (request.getStatus() != Friend.FriendStatus.PENDING) {
            throw new FriendException("이미 처리된 요청입니다.", HttpStatus.CONFLICT);
        }

        if (accept) {
            request.accept();
            eventPublisher.publishEvent(NotificationEvent.of(
                    request.getRequester().getId(), receiverId,
                    NotificationType.FRIEND_ACCEPTED,
                    "친구 요청 수락",
                    displayName(receiver) + "님이 친구 요청을 수락했습니다.",
                    receiver.getUuid(), "USER"
            ));
        } else {
            request.reject();
        }
    }

    // 보낸/받은 친구 요청 목록 조회
    @Transactional(readOnly = true)
    public List<FriendRequestResponse> getPendingRequests(Long userId) {
        User me = findUser(userId);

        List<Friend> sent = friendRepository.findByRequesterAndStatus(me, Friend.FriendStatus.PENDING);
        List<Friend> received = friendRepository.findByReceiverAndStatus(me, Friend.FriendStatus.PENDING);

        List<User> relatedUsers = new java.util.ArrayList<>();
        sent.stream().map(Friend::getReceiver).forEach(relatedUsers::add);
        received.stream().map(Friend::getRequester).forEach(relatedUsers::add);

        Map<Long, Profile> profileMap = buildProfileMap(relatedUsers);

        List<FriendRequestResponse> result = new java.util.ArrayList<>();
        sent.forEach(f -> result.add(FriendRequestResponse.ofSent(f, profileMap.get(f.getReceiver().getId()))));
        received.forEach(f -> result.add(FriendRequestResponse.ofReceived(f, profileMap.get(f.getRequester().getId()))));
        return result;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserException("존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND));
    }

    private java.util.Optional<User> findTargetUser(String targetUuid, String friendCode) {
        if (friendCode != null && !friendCode.isBlank()) {
            return userRepository.findByFriendCode(friendCode.toUpperCase(Locale.ROOT));
        }
        return userRepository.findByUuid(targetUuid);
    }

    private Map<Long, Profile> buildProfileMap(List<User> users) {
        return users.stream()
                .distinct()
                .map(u -> profileRepository.findByUser(u))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p));
    }

    private void publishFriendRequest(User requester, User target) {
        eventPublisher.publishEvent(NotificationEvent.of(
                target.getId(), requester.getId(),
                NotificationType.FRIEND_REQUESTED,
                "친구 요청",
                displayName(requester) + "님이 친구 요청을 보냈습니다.",
                requester.getUuid(), "USER"
        ));
    }

    private String displayName(User user) {
        return profileRepository.findByUser(user)
                .map(Profile::getNickname)
                .orElse(user.getUserId() != null ? user.getUserId() : "사용자");
    }

    // 닉네임 첫 글자의 그룹: 0=한글, 1=영어, 2=숫자, 3=특수기호
    private static int charGroup(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        if (block == Character.UnicodeBlock.HANGUL_SYLLABLES
                || block == Character.UnicodeBlock.HANGUL_JAMO
                || block == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO) {
            return 0;
        }
        if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) return 1;
        if (c >= '0' && c <= '9') return 2;
        return 3;
    }
}
