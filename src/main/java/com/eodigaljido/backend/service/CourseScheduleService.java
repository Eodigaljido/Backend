package com.eodigaljido.backend.service;

import com.eodigaljido.backend.domain.chat.ChatRoom;
import com.eodigaljido.backend.domain.chat.ChatRoomMember;
import com.eodigaljido.backend.domain.route.Route;
import com.eodigaljido.backend.domain.schedule.CourseSchedule;
import com.eodigaljido.backend.domain.schedule.CourseScheduleParticipant;
import com.eodigaljido.backend.domain.user.Profile;
import com.eodigaljido.backend.domain.user.User;
import com.eodigaljido.backend.dto.schedule.CourseScheduleListResponse;
import com.eodigaljido.backend.dto.schedule.CourseScheduleNearestResponse;
import com.eodigaljido.backend.dto.schedule.CourseScheduleResponse;
import com.eodigaljido.backend.dto.schedule.CreateCourseScheduleRequest;
import com.eodigaljido.backend.dto.schedule.ParticipantSummary;
import com.eodigaljido.backend.dto.schedule.UpdateCourseScheduleRequest;
import com.eodigaljido.backend.exception.CourseScheduleException;
import com.eodigaljido.backend.repository.ChatRoomMemberRepository;
import com.eodigaljido.backend.repository.ChatRoomRepository;
import com.eodigaljido.backend.repository.CourseScheduleParticipantRepository;
import com.eodigaljido.backend.repository.CourseScheduleRepository;
import com.eodigaljido.backend.repository.ProfileRepository;
import com.eodigaljido.backend.repository.RouteRepository;
import com.eodigaljido.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.eodigaljido.backend.domain.chat.ChatRoomMember.MemberRole.ADMIN;
import static com.eodigaljido.backend.domain.route.Route.RouteStatus.DELETED;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseScheduleService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final CourseScheduleRepository courseScheduleRepository;
    private final CourseScheduleParticipantRepository participantRepository;
    private final RouteRepository routeRepository;

    @Transactional
    public CourseScheduleResponse createSchedule(Long userId, CreateCourseScheduleRequest req) {
        User creator = findActiveUser(userId);
        ChatRoom chatRoom = resolveChatRoomForMember(req.chatRoomUuid(), creator);
        Route course = resolveCourse(req.courseUuid(), creator);

        CourseSchedule schedule = CourseSchedule.builder()
                .uuid(UUID.randomUUID().toString())
                .owner(creator)
                .title(normalizeTitle(req.title()))
                .scheduledAt(toUtcLocalDateTime(req.scheduledAt()))
                .chatRoom(chatRoom)
                .course(course)
                .build();
        courseScheduleRepository.save(schedule);
        snapshotParticipants(schedule, chatRoom);

        return toResponse(schedule);
    }

    public CourseScheduleListResponse getSchedules(Long userId, String from, String to,
                                                   String chatRoomUuid, boolean upcomingOnly) {
        User user = findActiveUser(userId);
        if (chatRoomUuid != null && !chatRoomUuid.isBlank()) {
            resolveChatRoomForMember(chatRoomUuid, user);
        }

        List<CourseSchedule> schedules = courseScheduleRepository.findAccessibleSchedules(
                user,
                toUtcStartOfDay(from),
                toUtcEndOfDay(to),
                blankToNull(chatRoomUuid),
                upcomingOnly,
                LocalDateTime.now(ZoneOffset.UTC)
        );

        return new CourseScheduleListResponse(schedules.stream()
                .map(this::toResponse)
                .toList());
    }

    public CourseScheduleNearestResponse getNearestSchedule(Long userId) {
        User user = findActiveUser(userId);
        List<CourseSchedule> result = courseScheduleRepository.findNearestAccessibleSchedule(
                user, LocalDateTime.now(ZoneOffset.UTC), PageRequest.of(0, 1));
        return new CourseScheduleNearestResponse(result.isEmpty() ? null : toResponse(result.get(0)));
    }

    public CourseScheduleResponse getSchedule(Long userId, String scheduleUuid) {
        User user = findActiveUser(userId);
        CourseSchedule schedule = findScheduleByUuid(scheduleUuid);
        validateCanView(schedule, user);
        return toResponse(schedule);
    }

    @Transactional
    public CourseScheduleResponse updateSchedule(Long userId, String scheduleUuid, UpdateCourseScheduleRequest req) {
        User user = findActiveUser(userId);
        CourseSchedule schedule = findScheduleByUuid(scheduleUuid);
        validateCanManage(schedule, user);

        if (req.title() != null) {
            schedule.updateTitle(normalizeTitle(req.title()));
        }
        if (req.scheduledAt() != null) {
            schedule.updateScheduledAt(toUtcLocalDateTime(req.scheduledAt()));
        }
        if (req.courseUuid() != null) {
            schedule.updateCourse(resolveCourse(req.courseUuid(), user));
        }
        if (req.chatRoomUuid() != null) {
            ChatRoom chatRoom = resolveChatRoomForMember(req.chatRoomUuid(), user);
            schedule.updateChatRoom(chatRoom);
            participantRepository.deleteBySchedule(schedule);
            snapshotParticipants(schedule, chatRoom);
        }

        return toResponse(schedule);
    }

    @Transactional
    public void deleteSchedule(Long userId, String scheduleUuid) {
        User user = findActiveUser(userId);
        CourseSchedule schedule = findScheduleByUuid(scheduleUuid);
        validateCanManage(schedule, user);
        schedule.delete();
    }

    private User findActiveUser(Long userId) {
        return userRepository.findById(userId)
                .filter(u -> u.getStatus() == User.UserStatus.ACTIVE)
                .orElseThrow(() -> new CourseScheduleException("User not found.", HttpStatus.NOT_FOUND));
    }

    private CourseSchedule findScheduleByUuid(String uuid) {
        return courseScheduleRepository.findByUuidAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new CourseScheduleException("Course schedule not found.", HttpStatus.NOT_FOUND));
    }

    private ChatRoom resolveChatRoomForMember(String chatRoomUuid, User user) {
        if (chatRoomUuid == null || chatRoomUuid.isBlank()) {
            throw new CourseScheduleException("chatRoomUuid is required.", HttpStatus.BAD_REQUEST);
        }

        ChatRoom chatRoom = chatRoomRepository.findByUuidAndDeletedAtIsNull(chatRoomUuid)
                .orElseThrow(() -> new CourseScheduleException("Chat room not found.", HttpStatus.NOT_FOUND));
        if (chatRoomMemberRepository.findByRoomAndUserAndLeftAtIsNull(chatRoom, user).isEmpty()) {
            throw new CourseScheduleException("You are not a member of this chat room.", HttpStatus.FORBIDDEN);
        }
        return chatRoom;
    }

    private Route resolveCourse(String courseUuid, User user) {
        if (courseUuid == null || courseUuid.isBlank()) {
            return null;
        }

        Route route = routeRepository.findByUuidAndStatusNot(courseUuid, DELETED)
                .orElseThrow(() -> new CourseScheduleException("Course not found.", HttpStatus.NOT_FOUND));
        boolean ownedByUser = route.getUser().getId().equals(user.getId());
        if (!route.isShared() && !ownedByUser) {
            throw new CourseScheduleException("You cannot access this course.", HttpStatus.FORBIDDEN);
        }
        return route;
    }

    private void validateCanView(CourseSchedule schedule, User user) {
        if (schedule.getOwner().getId().equals(user.getId())) {
            return;
        }
        if (schedule.getChatRoom() == null) {
            throw new CourseScheduleException("You cannot access this schedule.", HttpStatus.FORBIDDEN);
        }
        if (chatRoomMemberRepository.findByRoomAndUserAndLeftAtIsNull(schedule.getChatRoom(), user).isPresent()) {
            return;
        }
        throw new CourseScheduleException("You cannot access this schedule.", HttpStatus.FORBIDDEN);
    }

    private void validateCanManage(CourseSchedule schedule, User user) {
        if (schedule.getOwner().getId().equals(user.getId())) {
            return;
        }
        if (schedule.getChatRoom() == null) {
            throw new CourseScheduleException("You cannot modify this schedule.", HttpStatus.FORBIDDEN);
        }
        chatRoomMemberRepository.findByRoomAndUserAndLeftAtIsNull(schedule.getChatRoom(), user)
                .filter(member -> member.getRole() == ADMIN)
                .orElseThrow(() -> new CourseScheduleException("You cannot modify this schedule.", HttpStatus.FORBIDDEN));
    }

    private void snapshotParticipants(CourseSchedule schedule, ChatRoom chatRoom) {
        chatRoomMemberRepository.findByRoomAndLeftAtIsNull(chatRoom).forEach(member ->
                participantRepository.save(CourseScheduleParticipant.builder()
                        .schedule(schedule)
                        .user(member.getUser())
                        .source(CourseScheduleParticipant.ParticipantSource.CHAT_ROOM)
                        .build()));
    }

    private CourseScheduleResponse toResponse(CourseSchedule schedule) {
        List<ChatRoomMember> members = schedule.getChatRoom() != null
                ? chatRoomMemberRepository.findByRoomAndLeftAtIsNull(schedule.getChatRoom())
                : List.of();
        List<User> users = members.isEmpty()
                ? List.of(schedule.getOwner())
                : members.stream().map(ChatRoomMember::getUser).toList();
        Map<Long, Profile> profileMap = buildProfileMap(users);
        Profile creatorProfile = profileMap.get(schedule.getOwner().getId());

        return new CourseScheduleResponse(
                schedule.getUuid(),
                schedule.getTitle(),
                toOffsetDateTime(schedule.getScheduledAt()),
                schedule.getChatRoom() != null ? schedule.getChatRoom().getUuid() : null,
                schedule.getChatRoom() != null ? schedule.getChatRoom().getName() : null,
                schedule.getCourse() != null ? schedule.getCourse().getUuid() : null,
                schedule.getCourse() != null ? schedule.getCourse().getTitle() : null,
                schedule.getOwner().getUuid(),
                nicknameOf(schedule.getOwner(), creatorProfile),
                toParticipantSummaries(members, schedule.getOwner(), profileMap),
                toOffsetDateTime(schedule.getCreatedAt()),
                toOffsetDateTime(schedule.getUpdatedAt())
        );
    }

    private List<ParticipantSummary> toParticipantSummaries(List<ChatRoomMember> members, User owner, Map<Long, Profile> profileMap) {
        if (members.isEmpty()) {
            return List.of(new ParticipantSummary(
                    owner.getUuid(),
                    nicknameOf(owner, profileMap.get(owner.getId())),
                    owner.getUserId()
            ));
        }
        return members.stream()
                .map(member -> {
                    User user = member.getUser();
                    return new ParticipantSummary(
                            user.getUuid(),
                            nicknameOf(user, profileMap.get(user.getId())),
                            user.getUserId()
                    );
                })
                .toList();
    }

    private Map<Long, Profile> buildProfileMap(List<User> users) {
        if (users.isEmpty()) {
            return Map.of();
        }
        return profileRepository.findByUserIn(users).stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p, (first, second) -> first));
    }

    private String normalizeTitle(String title) {
        String normalized = title != null ? title.trim() : "";
        if (normalized.isBlank()) {
            throw new CourseScheduleException("title is required.", HttpStatus.BAD_REQUEST);
        }
        if (normalized.length() > 100) {
            throw new CourseScheduleException("title must be 100 characters or less.", HttpStatus.BAD_REQUEST);
        }
        return normalized;
    }

    private String nicknameOf(User user, Profile profile) {
        if (profile != null && profile.getNickname() != null && !profile.getNickname().isBlank()) {
            return profile.getNickname();
        }
        return user.getUserId();
    }

    private LocalDateTime toUtcLocalDateTime(OffsetDateTime scheduledAt) {
        return scheduledAt.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.atOffset(ZoneOffset.UTC) : null;
    }

    private LocalDateTime toUtcStartOfDay(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        return LocalDate.parse(date).atStartOfDay(KST)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
    }

    private LocalDateTime toUtcEndOfDay(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        return LocalDate.parse(date).atTime(LocalTime.MAX).atZone(KST)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
