package yooze.withme.domain.calendar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.calendar.dto.response.ScheduleResponse;
import yooze.withme.domain.calendar.entity.Schedule;
import yooze.withme.domain.calendar.repository.ScheduleRepository;

@ExtendWith(MockitoExtension.class)
class ScheduleQueryServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long SCHEDULE_ID = 10L;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private RecurrenceQueryService recurrenceQueryService;

    @InjectMocks
    private ScheduleQueryService scheduleQueryService;

    @Test
    void getScheduleReturnsOwnedActiveSchedule() {
        when(scheduleRepository.findById(SCHEDULE_ID))
                .thenReturn(Optional.of(schedule(USER_ID)));

        ScheduleResponse response = scheduleQueryService.getSchedule(USER_ID, SCHEDULE_ID);

        assertThat(response.scheduleId()).isEqualTo(SCHEDULE_ID);
        assertThat(response.title()).isEqualTo("치과");
    }

    @Test
    void getScheduleThrowsWhenScheduleDoesNotExist() {
        when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scheduleQueryService.getSchedule(USER_ID, SCHEDULE_ID))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getErrorStatus())
                .isEqualTo(ErrorStatus.SCHEDULE_NOT_FOUND);
    }

    @Test
    void getScheduleRejectsAnotherUsersSchedule() {
        when(scheduleRepository.findById(SCHEDULE_ID))
                .thenReturn(Optional.of(schedule(2L)));

        assertThatThrownBy(() -> scheduleQueryService.getSchedule(USER_ID, SCHEDULE_ID))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getErrorStatus())
                .isEqualTo(ErrorStatus.SCHEDULE_FORBIDDEN);
    }

    @Test
    void getScheduleTreatsDeletedScheduleAsNotFound() {
        Schedule deleted = Schedule.builder()
                .scheduleId(SCHEDULE_ID)
                .user(User.builder().userId(USER_ID).nickname("사용자").build())
                .title("삭제된 일정")
                .allDay(true)
                .startDate(LocalDate.of(2026, 8, 20))
                .endDate(LocalDate.of(2026, 8, 20))
                .deletedAt(LocalDateTime.now())
                .build();
        when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> scheduleQueryService.getSchedule(USER_ID, SCHEDULE_ID))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getErrorStatus())
                .isEqualTo(ErrorStatus.SCHEDULE_NOT_FOUND);
    }

    private Schedule schedule(Long userId) {
        return Schedule.builder()
                .scheduleId(SCHEDULE_ID)
                .user(User.builder().userId(userId).nickname("사용자").build())
                .title("치과")
                .allDay(false)
                .startDate(LocalDate.of(2026, 8, 20))
                .startTime(LocalTime.of(14, 0))
                .endDate(LocalDate.of(2026, 8, 20))
                .endTime(LocalTime.of(15, 0))
                .build();
    }
}
