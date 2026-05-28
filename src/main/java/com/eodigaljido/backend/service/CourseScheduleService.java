package com.eodigaljido.backend.service;

import com.eodigaljido.backend.domain.chat.ChatRoom;
import com.eodigaljido.backend.domain.chat.ChatRoomMember;
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
import com.eodigaljido.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseScheduleService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final CourseScheduleRepository courseScheduleRepository;
    private final CourseScheduleParticipantRepository participantRepository;

    @Transactional
    public CourseScheduleResponse createSchedule(Long userId, CreateCourseScheduleRequest req) {
        User owner = findUser(userId);
        LocalDateTime scheduledAt = parseScheduledAt(req.date(), req.time());

        ChatRoom chatRoom = resolveChatRoom(req.chatRoomUuid(), owner);

        CourseSchedule schedule = CourseSchedule.builder()
                .uuid(UUID.randomUUID().toString())
                .owner(owner)
                .title(req.title())
                .scheduledAt(scheduledAt)
                .chatRoom(chatRoom)
                .memo(req.memo())
                .build();
        courseScheduleRepository.save(schedule);

        snapshotParticipants(schedule, owner, chatRoom);

        List<CourseScheduleParticipant> participants = participantRepository.findByScheduleWithUser(schedule);
        Map<Long, Profile> profileMap = buildProfileMap(participants.stream().map(CourseScheduleParticipant::getUser).toList());

        return toResponse(schedule, participants, profileMap);
    }

    public CourseScheduleListResponse getSchedules(Long userId, String from, String to, int page, int size) {
        User owner = findUser(userId);
        LocalDateTime fromDt = from != null ? LocalDate.parse(from).atStartOfDay() : null;
        LocalDateTime toDt = to != null ? LocalDate.parse(to).atTime(LocalTime.MAX) : null;

        Page<CourseSchedule> schedulePage = courseScheduleRepository.findByOwnerAndDateRange(
                owner, fromDt, toDt, PageRequest.of(page, size));

        List<CourseSchedule> schedules = schedulePage.getContent();
        List<CourseScheduleParticipant> allParticipants = schedules.isEmpty()
                ? List.of()
                : participantRepository.findByScheduleInWithUser(schedules);

        Map<Long, Profile> profileMap = buildProfileMap(
                allParticipants.stream().map(CourseScheduleParticipant::getUser).toList());

        Map<Long, List<CourseScheduleParticipant>> bySchedule = allParticipants.stream()
                .collect(Collectors.groupingBy(p -> p.getSchedule().getId()));

        List<CourseScheduleListResponse.CourseScheduleItem> items = schedules.stream()
                .map(s -> toListItem(s, bySchedule.getOrDefault(s.getId(), List.of()), profileMap))
                .toList();

        return new CourseScheduleListResponse(items, page, size, schedulePage.getTotalElements());
    }

    public CourseScheduleNearestResponse getNearestSchedule(Long userId) {
        User owner = findUser(userId);
        Page<CourseSchedule> result = courseScheduleRepository.findNearestByOwner(
                owner, LocalDateTime.now(), PageRequest.of(0, 1));

        if (result.isEmpty()) {
            return null;
        }

        CourseSchedule schedule = result.getContent().get(0);
        long count = participantRepository.countBySchedule(schedule);

        return new CourseScheduleNearestResponse(
                schedule.getUuid(),
                schedule.getTitle(),
                schedule.getScheduledAt(),
                schedule.getChatRoom() != null ? schedule.getChatRoom().getUuid() : null,
                schedule.getChatRoom() != null ? schedule.getChatRoom().getName() : null,
                count
        );
    }

    public CourseScheduleResponse getSchedule(Long userId, String scheduleUuid) {
        User owner = findUser(userId);
        CourseSchedule schedule = findScheduleByUuid(scheduleUuid);
        validateOwner(schedule, owner);

        List<CourseScheduleParticipant> participants = participantRepository.findByScheduleWithUser(schedule);
        Map<Long, Profile> profileMap = buildProfileMap(participants.stream().map(CourseScheduleParticipant::getUser).toList());

        return toResponse(schedule, participants, profileMap);
    }

    @Transactional
    public CourseScheduleResponse updateSchedule(Long userId, String scheduleUuid, UpdateCourseScheduleRequest req) {
        User owner = findUser(userId);
        CourseSchedule schedule = findScheduleByUuid(scheduleUuid);
        validateOwner(schedule, owner);

        if (req.title() != null) {
            schedule.updateTitle(req.title());
        }

        if (req.date() != null || req.time() != null) {
            String date = req.date() != null ? req.date()
                    : schedule.getScheduledAt().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
            String time = req.time() != null ? req.time()
                    : schedule.getScheduledAt().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"));
            schedule.updateScheduledAt(parseScheduledAt(date, time));
        }

        if (req.memo() != null) {
            schedule.updateMemo(req.memo());
        }

        boolean chatRoomChanged = req.chatRoomUuid() != null;
        if (chatRoomChanged) {
            ChatRoom newChatRoom = resolveChatRoom(req.chatRoomUuid().isBlank() ? null : req.chatRoomUuid(), owner);
            schedule.updateChatRoom(newChatRoom);
            participantRepository.deleteBySchedule(schedule);
            snapshotParticipants(schedule, owner, newChatRoom);
        }

        List<CourseScheduleParticipant> participants = participantRepository.findByScheduleWithUser(schedule);
        Map<Long, Profile> profileMap = buildProfileMap(participants.stream().map(CourseScheduleParticipant::getUser).toList());

        return toResponse(schedule, participants, profileMap);
    }

    @Transactional
    public void deleteSchedule(Long userId, String scheduleUuid) {
        User owner = findUser(userId);
        CourseSchedule schedule = findScheduleByUuid(scheduleUuid);
        validateOwner(schedule, owner);
        schedule.delete();
    }

    // --- 내부 헬퍼 ---

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CourseScheduleException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    private CourseSchedule findScheduleByUuid(String uuid) {
        return courseScheduleRepository.findByUuidAndDeletedAtIsNull(uuid)
                .orElseThrow(() -> new CourseScheduleException("약속을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    private void validateOwner(CourseSchedule schedule, User user) {
        if (!schedule.getOwner().getId().equals(user.getId())) {
            throw new CourseScheduleException("해당 약속에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
    }

    private ChatRoom resolveChatRoom(String chatRoomUuid, User owner) {
        if (chatRoomUuid == null || chatRoomUuid.isBlank()) {
            return null;
        }
        ChatRoom chatRoom = chatRoomRepository.findByUuidAndDeletedAtIsNull(chatRoomUuid)
                .orElseThrow(() -> new CourseScheduleException("채팅방을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        boolean isMember = chatRoomMemberRepository
                .findByRoomAndUserAndLeftAtIsNull(chatRoom, owner)
                .isPresent();
        if (!isMember) {
            throw new CourseScheduleException("해당 채팅방에 접근할 수 없습니다.", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return chatRoom;
    }

    private void snapshotParticipants(CourseSchedule schedule, User owner, ChatRoom chatRoom) {
        if (chatRoom != null) {
            List<ChatRoomMember> members = chatRoomMemberRepository.findByRoomAndLeftAtIsNull(chatRoom);
            for (ChatRoomMember member : members) {
                participantRepository.save(CourseScheduleParticipant.builder()
                        .schedule(schedule)
                        .user(member.getUser())
                        .source(CourseScheduleParticipant.ParticipantSource.CHAT_ROOM)
                        .build());
            }
        } else {
            participantRepository.save(CourseScheduleParticipant.builder()
                    .schedule(schedule)
                    .user(owner)
                    .source(CourseScheduleParticipant.ParticipantSource.MANUAL)
                    .build());
        }
    }

    private LocalDateTime parseScheduledAt(String date, String time) {
        try {
            LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
            LocalTime localTime = LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));
            return LocalDateTime.of(localDate, localTime);
        } catch (DateTimeParseException e) {
            throw new CourseScheduleException("날짜 또는 시간 형식이 올바르지 않습니다. (date: yyyy-MM-dd, time: HH:mm)", HttpStatus.BAD_REQUEST);
        }
    }

    private Map<Long, Profile> buildProfileMap(List<User> users) {
        if (users.isEmpty()) return Map.of();
        List<Profile> profiles = profileRepository.findByUserIn(users);
        return profiles.stream().collect(Collectors.toMap(p -> p.getUser().getId(), p -> p));
    }

    private String nicknameOf(User user, Map<Long, Profile> profileMap) {
        Profile profile = profileMap.get(user.getId());
        return profile != null ? profile.getNickname() : user.getUserId();
    }

    private CourseScheduleResponse toResponse(CourseSchedule s, List<CourseScheduleParticipant> participants, Map<Long, Profile> profileMap) {
        List<ParticipantSummary> summaries = participants.stream()
                .map(p -> new ParticipantSummary(p.getUser().getUuid(), nicknameOf(p.getUser(), profileMap)))
                .toList();

        return new CourseScheduleResponse(
                s.getUuid(),
                s.getTitle(),
                s.getScheduledAt(),
                s.getChatRoom() != null ? s.getChatRoom().getUuid() : null,
                s.getChatRoom() != null ? s.getChatRoom().getName() : null,
                s.getMemo(),
                summaries,
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }

    private CourseScheduleListResponse.CourseScheduleItem toListItem(CourseSchedule s, List<CourseScheduleParticipant> participants, Map<Long, Profile> profileMap) {
        List<ParticipantSummary> summaries = participants.stream()
                .map(p -> new ParticipantSummary(p.getUser().getUuid(), nicknameOf(p.getUser(), profileMap)))
                .toList();

        return new CourseScheduleListResponse.CourseScheduleItem(
                s.getUuid(),
                s.getTitle(),
                s.getScheduledAt(),
                s.getChatRoom() != null ? s.getChatRoom().getUuid() : null,
                s.getChatRoom() != null ? s.getChatRoom().getName() : null,
                summaries
        );
    }
}
