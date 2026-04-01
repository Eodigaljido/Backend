package com.eodigaljido.backend.service;

import com.eodigaljido.backend.domain.friend.Friend;
import com.eodigaljido.backend.domain.user.Profile;
import com.eodigaljido.backend.domain.user.User;
import com.eodigaljido.backend.dto.friend.FriendRequestResponse;
import com.eodigaljido.backend.dto.friend.FriendResponse;
import com.eodigaljido.backend.exception.FriendException;
import com.eodigaljido.backend.exception.UserException;
import com.eodigaljido.backend.repository.FriendRepository;
import com.eodigaljido.backend.repository.ProfileRepository;
import com.eodigaljido.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendRepository friendRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    // 친구 목록 조회
    @Transactional(readOnly = true)
    public List<FriendResponse> getFriends(Long userId) {
        User me = findUser(userId);
        List<Friend> friends = friendRepository.findAcceptedFriends(me);

        List<User> others = friends.stream()
                .map(f -> f.getRequester().getId().equals(me.getId()) ? f.getReceiver() : f.getRequester())
                .toList();

        Map<Long, Profile> profileMap = buildProfileMap(others);

        return friends.stream()
                .map(f -> {
                    User other = f.getRequester().getId().equals(me.getId()) ? f.getReceiver() : f.getRequester();
                    return FriendResponse.of(f, me, profileMap.get(other.getId()));
                })
                .toList();
    }

    // 친구 요청 전송
    @Transactional
    public void sendRequest(Long requesterId, String targetUuid) {
        User requester = findUser(requesterId);
        User target = userRepository.findByUuid(targetUuid)
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

    private Map<Long, Profile> buildProfileMap(List<User> users) {
        return users.stream()
                .distinct()
                .map(u -> profileRepository.findByUser(u))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p));
    }
}
