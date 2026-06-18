package com.eodigaljido.backend.service;

import com.eodigaljido.backend.domain.chat.ChatMessage;
import com.eodigaljido.backend.domain.schedule.CourseSchedule;
import com.eodigaljido.backend.domain.user.User;
import com.eodigaljido.backend.dto.schedule.CourseScheduleResponse;
import com.eodigaljido.backend.dto.schedule.CreateCourseScheduleRequest;
import com.eodigaljido.backend.repository.ChatMessageRepository;
import com.eodigaljido.backend.repository.ChatRoomMemberRepository;
import com.eodigaljido.backend.repository.ChatRoomRepository;
import com.eodigaljido.backend.repository.CourseScheduleParticipantRepository;
import com.eodigaljido.backend.repository.CourseScheduleRepository;
import com.eodigaljido.backend.repository.ProfileRepository;
import com.eodigaljido.backend.repository.RouteRepository;
import com.eodigaljido.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseScheduleServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final ProfileRepository profileRepository = mock(ProfileRepository.class);
    private final ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
    private final ChatRoomMemberRepository chatRoomMemberRepository = mock(ChatRoomMemberRepository.class);
    private final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
    private final CourseScheduleRepository courseScheduleRepository = mock(CourseScheduleRepository.class);
    private final CourseScheduleParticipantRepository participantRepository = mock(CourseScheduleParticipantRepository.class);
    private final RouteRepository routeRepository = mock(RouteRepository.class);
    private final CourseScheduleService service = new CourseScheduleService(
            userRepository,
            profileRepository,
            chatRoomRepository,
            chatRoomMemberRepository,
            chatMessageRepository,
            courseScheduleRepository,
            participantRepository,
            routeRepository
    );

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .uuid("user-uuid")
                .userId("planner")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(profileRepository.findByUserIn(any())).thenReturn(List.of());
    }

    @Test
    void createsScheduleWithoutChatRoom() {
        CreateCourseScheduleRequest request = new CreateCourseScheduleRequest(
                "  Coffee walk  ",
                OffsetDateTime.parse("2026-06-08T19:00:00+09:00"),
                null,
                null,
                true
        );

        CourseScheduleResponse response = service.createSchedule(1L, request);

        assertThat(response.title()).isEqualTo("Coffee walk");
        assertThat(response.chatRoomUuid()).isNull();
        assertThat(response.chatRoomName()).isNull();
        assertThat(response.participants()).singleElement().satisfies(participant -> {
            assertThat(participant.uuid()).isEqualTo("user-uuid");
            assertThat(participant.nickname()).isEqualTo("planner");
            assertThat(participant.userId()).isEqualTo("planner");
        });

        ArgumentCaptor<CourseSchedule> scheduleCaptor = ArgumentCaptor.forClass(CourseSchedule.class);
        verify(courseScheduleRepository).save(scheduleCaptor.capture());
        assertThat(scheduleCaptor.getValue().getChatRoom()).isNull();
        verify(chatRoomRepository, never()).findByUuidAndDeletedAtIsNull(any());
        verify(participantRepository, never()).save(any());
        verify(chatMessageRepository, never()).save(any(ChatMessage.class));
    }
}
