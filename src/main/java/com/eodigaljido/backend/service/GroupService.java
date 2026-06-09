package com.eodigaljido.backend.service;

import com.eodigaljido.backend.domain.chat.ChatRoom;
import com.eodigaljido.backend.domain.chat.ChatRoomMember;
import com.eodigaljido.backend.domain.group.Group;
import com.eodigaljido.backend.domain.group.GroupJoinRequest;
import com.eodigaljido.backend.domain.group.GroupMember;
import com.eodigaljido.backend.domain.group.GroupPost;
import com.eodigaljido.backend.domain.notification.NotificationType;
import com.eodigaljido.backend.domain.route.Route;
import com.eodigaljido.backend.domain.user.User;
import com.eodigaljido.backend.dto.course.CourseItemResponse;
import com.eodigaljido.backend.dto.course.CourseStepResponse;
import com.eodigaljido.backend.dto.group.*;
import com.eodigaljido.backend.event.NotificationEvent;
import com.eodigaljido.backend.exception.GroupException;
import com.eodigaljido.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupJoinRequestRepository groupJoinRequestRepository;
    private final GroupPostRepository groupPostRepository;
    private final RouteRepository routeRepository;
    private final RouteWaypointRepository waypointRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final FileStorageService fileStorageService;

    // ──────────────────────────────────────────────────────────
    // 모임 생성
    // ──────────────────────────────────────────────────────────

    @Transactional
    public GroupResponse createGroup(Long userId, CreateGroupRequest req, MultipartFile image) {
        User creator = findUser(userId);

        String groupUuid = UUID.randomUUID().toString();
        String profileImageUrl = uploadGroupImage(image, groupUuid);

        Group.GroupType type = req.type() != null ? req.type() : Group.GroupType.PUBLIC;
        boolean requiresApproval = Boolean.TRUE.equals(req.requiresApproval());

        Group group = Group.builder()
                .uuid(groupUuid)
                .name(req.name())
                .description(req.description())
                .profileImageUrl(profileImageUrl)
                .type(type)
                .requiresApproval(requiresApproval)
                .createdBy(creator)
                .build();
        groupRepository.save(group);

        groupMemberRepository.save(GroupMember.builder()
                .group(group)
                .user(creator)
                .role(GroupMember.MemberRole.ADMIN)
                .build());

        return GroupResponse.of(group, 1L, true);
    }

    // ──────────────────────────────────────────────────────────
    // 모임 목록 조회 (공개)
    // ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<GroupResponse> getPublicGroups(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Group> groups = groupRepository.findPublicGroupsOrderByViewCount(pageable);
        return groups.map(g -> {
            long memberCount = groupMemberRepository.countByGroupAndLeftAtIsNull(g);
            boolean joinedByMe = userId != null && isMember(g, userId);
            return GroupResponse.of(g, memberCount, joinedByMe);
        });
    }

    // ──────────────────────────────────────────────────────────
    // 모임 상세 조회 (조회수 증가)
    // ──────────────────────────────────────────────────────────

    @Transactional
    public GroupDetailResponse getGroup(String groupUuid, Long userId) {
        Group group = findActiveGroup(groupUuid);
        group.incrementViewCount();
        long memberCount = groupMemberRepository.countByGroupAndLeftAtIsNull(group);
        boolean joinedByMe = userId != null && isMember(group, userId);
        List<GroupMemberResponse> members = groupMemberRepository.findAllActiveMembers(group)
                .stream()
                .map(m -> GroupMemberResponse.of(m, profileRepository.findByUser(m.getUser()).orElse(null)))
                .toList();
        return GroupDetailResponse.of(group, memberCount, joinedByMe, members);
    }

    // ──────────────────────────────────────────────────────────
    // 모임 수정 (방장만)
    // ──────────────────────────────────────────────────────────

    @Transactional
    public GroupResponse updateGroup(Long userId, String groupUuid, CreateGroupRequest req) {
        Group group = findActiveGroup(groupUuid);
        requireAdmin(group, userId);

        Group.GroupType type = req.type() != null ? req.type() : group.getType();
        boolean requiresApproval = req.requiresApproval() != null ? req.requiresApproval() : group.isRequiresApproval();
        group.update(req.name(), req.description(), group.getProfileImageUrl(), type, requiresApproval);

        long memberCount = groupMemberRepository.countByGroupAndLeftAtIsNull(group);
        return GroupResponse.of(group, memberCount, true);
    }

    // ──────────────────────────────────────────────────────────
    // 모임 삭제 (방장만)
    // ──────────────────────────────────────────────────────────

    @Transactional
    public void deleteGroup(Long userId, String groupUuid) {
        Group group = findActiveGroup(groupUuid);
        requireAdmin(group, userId);
        group.delete();
    }

    // ──────────────────────────────────────────────────────────
    // 모임 가입
    // ──────────────────────────────────────────────────────────

    @Transactional
    public GroupJoinResponse joinGroup(Long userId, String groupUuid) {
        Group group = findActiveGroup(groupUuid);
        User user = findUser(userId);

        if (group.getType() == Group.GroupType.PRIVATE) {
            throw new GroupException("비공개 모임은 초대를 통해서만 가입할 수 있습니다.", HttpStatus.FORBIDDEN);
        }

        if (isMember(group, userId)) {
            throw new GroupException("이미 모임에 참여 중입니다.", HttpStatus.CONFLICT);
        }

        if (group.isRequiresApproval()) {
            if (groupJoinRequestRepository.existsByGroupAndRequesterAndStatus(
                    group, user, GroupJoinRequest.RequestStatus.PENDING)) {
                throw new GroupException("이미 처리 대기 중인 가입 요청이 있습니다.", HttpStatus.CONFLICT);
            }
            groupJoinRequestRepository.save(GroupJoinRequest.builder()
                    .group(group)
                    .requester(user)
                    .build());
            notifyAdmin(group, user, NotificationType.GROUP_JOIN_REQUESTED,
                    "모임 가입 요청",
                    user.getUserId() + "님이 '" + group.getName() + "' 모임 가입을 요청했습니다.",
                    group.getUuid());
            return new GroupJoinResponse("PENDING");
        } else {
            addMember(group, user);
            return new GroupJoinResponse("JOINED");
        }
    }

    // ──────────────────────────────────────────────────────────
    // 모임 탈퇴
    // ──────────────────────────────────────────────────────────

    @Transactional
    public void leaveGroup(Long userId, String groupUuid) {
        Group group = findActiveGroup(groupUuid);
        if (group.getCreatedBy().getId().equals(userId)) {
            throw new GroupException("방장은 모임을 탈퇴할 수 없습니다. 모임을 삭제하거나 방장을 위임하세요.", HttpStatus.BAD_REQUEST);
        }
        User user = findUser(userId);
        GroupMember member = groupMemberRepository.findByGroupAndUserAndLeftAtIsNull(group, user)
                .orElseThrow(() -> new GroupException("모임 멤버가 아닙니다.", HttpStatus.NOT_FOUND));
        member.leave();
    }

    // ──────────────────────────────────────────────────────────
    // 가입 요청 목록 (방장)
    // ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<GroupJoinRequestResponse> getJoinRequests(Long userId, String groupUuid, int page, int size) {
        Group group = findActiveGroup(groupUuid);
        requireAdmin(group, userId);
        Pageable pageable = PageRequest.of(page, size);
        return groupJoinRequestRepository.findPendingRequests(group, pageable)
                .map(req -> GroupJoinRequestResponse.of(req,
                        profileRepository.findByUser(req.getRequester()).orElse(null)));
    }

    // ──────────────────────────────────────────────────────────
    // 가입 요청 승인/거절 (방장)
    // ──────────────────────────────────────────────────────────

    @Transactional
    public void processJoinRequest(Long adminId, Long requestId, ProcessGroupJoinRequest.Action action) {
        GroupJoinRequest request = groupJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new GroupException("가입 요청을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        Group group = request.getGroup();
        requireAdmin(group, adminId);

        if (request.getStatus() != GroupJoinRequest.RequestStatus.PENDING) {
            throw new GroupException("이미 처리된 요청입니다.", HttpStatus.CONFLICT);
        }

        User admin = findUser(adminId);
        String adminNickname = profileRepository.findByUser(admin)
                .map(p -> p.getNickname()).orElse(admin.getUserId());

        if (action == ProcessGroupJoinRequest.Action.APPROVE) {
            request.approve(admin);
            addMember(group, request.getRequester());
            eventPublisher.publishEvent(NotificationEvent.of(
                    request.getRequester().getId(), adminId,
                    NotificationType.GROUP_JOIN_APPROVED,
                    "모임 가입 승인",
                    adminNickname + "님이 '" + group.getName() + "' 모임 가입을 승인했습니다.",
                    group.getUuid(), "GROUP"
            ));
        } else {
            request.reject(admin);
            eventPublisher.publishEvent(NotificationEvent.of(
                    request.getRequester().getId(), adminId,
                    NotificationType.GROUP_JOIN_REJECTED,
                    "모임 가입 거절",
                    adminNickname + "님이 '" + group.getName() + "' 모임 가입을 거절했습니다.",
                    group.getUuid(), "GROUP"
            ));
        }
    }

    // ──────────────────────────────────────────────────────────
    // 내가 속한 모임 목록
    // ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<MyGroupResponse> getMyGroups(Long userId, int page, int size) {
        User user = findUser(userId);
        Pageable pageable = PageRequest.of(page, size);
        return groupMemberRepository.findActiveGroupsByUser(user, pageable)
                .map(gm -> {
                    Group g = gm.getGroup();
                    long memberCount = groupMemberRepository.countByGroupAndLeftAtIsNull(g);
                    return MyGroupResponse.of(g, memberCount);
                });
    }

    // ──────────────────────────────────────────────────────────
    // 멤버 목록
    // ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<GroupMemberResponse> getMembers(String groupUuid, int page, int size) {
        Group group = findActiveGroup(groupUuid);
        Pageable pageable = PageRequest.of(page, size);
        return groupMemberRepository.findActiveMembers(group, pageable)
                .map(m -> GroupMemberResponse.of(m,
                        profileRepository.findByUser(m.getUser()).orElse(null)));
    }

    // ──────────────────────────────────────────────────────────
    // 채팅방 생성 (방장만)
    // ──────────────────────────────────────────────────────────

    @Transactional
    public GroupChatRoomResponse createChatRoom(Long userId, String groupUuid, CreateGroupChatRoomRequest req) {
        Group group = findActiveGroup(groupUuid);
        requireAdmin(group, userId);

        User creator = findUser(userId);
        ChatRoom room = ChatRoom.builder()
                .uuid(UUID.randomUUID().toString())
                .name(req.name())
                .type(ChatRoom.RoomType.GROUP)
                .createdBy(creator)
                .build();
        room.assignGroup(group);
        chatRoomRepository.save(room);

        chatRoomMemberRepository.save(ChatRoomMember.builder()
                .room(room)
                .user(creator)
                .role(ChatRoomMember.MemberRole.ADMIN)
                .build());

        notifyGroupMembers(
                group, creator, NotificationType.GROUP_CHAT_ROOM_CREATED,
                "새 채팅방",
                "'" + room.getName() + "' 채팅방이 생성되었습니다.",
                room.getUuid(), "CHAT_ROOM"
        );
        return GroupChatRoomResponse.from(room);
    }

    // ──────────────────────────────────────────────────────────
    // 채팅방 목록
    // ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<GroupChatRoomResponse> getChatRooms(Long userId, String groupUuid) {
        Group group = findActiveGroup(groupUuid);
        requireMember(group, userId);
        return chatRoomRepository.findByGroupAndDeletedAtIsNull(group)
                .stream().map(GroupChatRoomResponse::from).toList();
    }

    // ──────────────────────────────────────────────────────────
    // 게시물 작성
    // ──────────────────────────────────────────────────────────

    @Transactional
    public GroupPostResponse createPost(Long userId, String groupUuid, CreateGroupPostRequest req) {
        Group group = findActiveGroup(groupUuid);
        requireMember(group, userId);
        User author = findUser(userId);

        GroupPost post = GroupPost.builder()
                .uuid(UUID.randomUUID().toString())
                .group(group)
                .author(author)
                .content(req.content())
                .build();
        if (req.imageUrls() != null) {
            post.update(req.content(), req.imageUrls());
        }
        groupPostRepository.save(post);

        notifyGroupMembers(
                group, author, NotificationType.GROUP_POST_CREATED,
                "새 게시물",
                displayName(author) + "님이 새 게시물을 작성했습니다.",
                post.getUuid(), "GROUP_POST"
        );
        return GroupPostResponse.of(post, profileRepository.findByUser(author).orElse(null));
    }

    // ──────────────────────────────────────────────────────────
    // 게시물 목록
    // ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<GroupPostResponse> getPosts(Long userId, String groupUuid, int page, int size) {
        Group group = findActiveGroup(groupUuid);
        requireMember(group, userId);
        Pageable pageable = PageRequest.of(page, size);
        return groupPostRepository.findByGroupAndDeletedAtIsNull(group, pageable)
                .map(p -> GroupPostResponse.of(p,
                        profileRepository.findByUser(p.getAuthor()).orElse(null)));
    }

    // ──────────────────────────────────────────────────────────
    // 게시물 수정
    // ──────────────────────────────────────────────────────────

    @Transactional
    public GroupPostResponse updatePost(Long userId, String postUuid, CreateGroupPostRequest req) {
        GroupPost post = groupPostRepository.findByUuidAndDeletedAtIsNull(postUuid)
                .orElseThrow(() -> new GroupException("게시물을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (!post.getAuthor().getId().equals(userId)) {
            throw new GroupException("게시물 작성자만 수정할 수 있습니다.", HttpStatus.FORBIDDEN);
        }
        post.update(req.content(), req.imageUrls());
        return GroupPostResponse.of(post, profileRepository.findByUser(post.getAuthor()).orElse(null));
    }

    // ──────────────────────────────────────────────────────────
    // 게시물 삭제
    // ──────────────────────────────────────────────────────────

    @Transactional
    public void deletePost(Long userId, String groupUuid, String postUuid) {
        Group group = findActiveGroup(groupUuid);
        GroupPost post = groupPostRepository.findByUuidAndDeletedAtIsNull(postUuid)
                .orElseThrow(() -> new GroupException("게시물을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        boolean isAdmin = isAdmin(group, userId);
        if (!post.getAuthor().getId().equals(userId) && !isAdmin) {
            throw new GroupException("삭제 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
        post.delete();
    }

    // ──────────────────────────────────────────────────────────
    // 모임 내 루트 목록
    // ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CourseItemResponse> getGroupRoutes(Long userId, String groupUuid) {
        Group group = findActiveGroup(groupUuid);
        requireMember(group, userId);

        List<Route> routes = routeRepository.findByGroupAndStatusNot(group, Route.RouteStatus.DELETED);
        return routes.stream().map(r -> {
            List<CourseStepResponse> steps = waypointRepository.findByRouteOrderBySequenceAsc(r)
                    .stream().map(CourseStepResponse::from).toList();
            return CourseItemResponse.of(r, steps);
        }).toList();
    }

    // ──────────────────────────────────────────────────────────
    // private helpers
    // ──────────────────────────────────────────────────────────

    private Group findActiveGroup(String uuid) {
        return groupRepository.findByUuidAndStatusNot(uuid, Group.GroupStatus.DELETED)
                .orElseThrow(() -> new GroupException("모임을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GroupException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    private boolean isMember(Group group, Long userId) {
        return userRepository.findById(userId)
                .map(u -> groupMemberRepository.existsByGroupAndUserAndLeftAtIsNull(group, u))
                .orElse(false);
    }

    private boolean isAdmin(Group group, Long userId) {
        return group.getCreatedBy().getId().equals(userId);
    }

    private void requireAdmin(Group group, Long userId) {
        if (!isAdmin(group, userId)) {
            throw new GroupException("방장만 수행할 수 있습니다.", HttpStatus.FORBIDDEN);
        }
    }

    private void requireMember(Group group, Long userId) {
        if (!isMember(group, userId)) {
            throw new GroupException("모임 멤버가 아닙니다.", HttpStatus.FORBIDDEN);
        }
    }

    private void addMember(Group group, User user) {
        groupMemberRepository.findByGroupAndUser(group, user)
                .ifPresentOrElse(
                        GroupMember::rejoin,
                        () -> groupMemberRepository.save(GroupMember.builder()
                                .group(group)
                                .user(user)
                                .role(GroupMember.MemberRole.MEMBER)
                                .build())
                );
        notifyGroupMembers(
                group, user, NotificationType.GROUP_MEMBER_JOINED,
                "새 멤버 참여",
                displayName(user) + "님이 '" + group.getName() + "' 모임에 참여했습니다.",
                group.getUuid(), "GROUP"
        );
    }

    private void notifyGroupMembers(Group group, User actor, NotificationType type,
                                    String title, String body, String referenceId, String referenceType) {
        groupMemberRepository.findAllActiveMembers(group).stream()
                .map(GroupMember::getUser)
                .filter(member -> !member.getId().equals(actor.getId()))
                .forEach(member -> eventPublisher.publishEvent(NotificationEvent.of(
                        member.getId(), actor.getId(), type, title, body, referenceId, referenceType
                )));
    }

    private String displayName(User user) {
        return profileRepository.findByUser(user)
                .map(profile -> profile.getNickname())
                .orElse(user.getUserId() != null ? user.getUserId() : "사용자");
    }

    private String uploadGroupImage(MultipartFile image, String groupUuid) {
        if (image == null || image.isEmpty()) return null;
        try {
            return fileStorageService.store(image, "groups", groupUuid);
        } catch (IllegalStateException e) {
            throw new GroupException("파일 저장소 설정이 누락되었습니다. " + e.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        } catch (IllegalArgumentException e) {
            throw new GroupException(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            throw new GroupException("이미지 업로드 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void notifyAdmin(Group group, User actor, NotificationType type,
                             String title, String body, String targetUuid) {
        eventPublisher.publishEvent(NotificationEvent.of(
                group.getCreatedBy().getId(), actor.getId(),
                type, title, body, targetUuid, "GROUP"
        ));
    }

}
