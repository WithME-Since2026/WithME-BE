package yooze.withme.domain.calendar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.auth.service.UserQueryService;
import yooze.withme.domain.calendar.dto.request.CreateScheduleRequest;
import yooze.withme.domain.calendar.dto.request.RecurrenceRequest;
import yooze.withme.domain.calendar.dto.request.UpdateScheduleRequest;
import yooze.withme.domain.calendar.dto.response.RecurrenceResponse;
import yooze.withme.domain.calendar.dto.response.ScheduleResponse;
import yooze.withme.domain.calendar.entity.Schedule;
import yooze.withme.domain.calendar.enums.RecurrenceEndType;
import yooze.withme.domain.calendar.enums.RecurrenceFreq;
import yooze.withme.domain.calendar.enums.RecurrenceOwnerType;
import yooze.withme.domain.calendar.repository.ScheduleRepository;

@ExtendWith(MockitoExtension.class)
class ScheduleCommandServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long SCHEDULE_ID = 10L;
    private static final LocalDate DATE = LocalDate.of(2026, 8, 20);

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ScheduleQueryService scheduleQueryService;

    @Mock
    private RecurrenceCommandService recurrenceCommandService;

    @Mock
    private UserQueryService userQueryService;

    @InjectMocks
    private ScheduleCommandService scheduleCommandService;

    @Test
    void createTimedScheduleSavesTimes() {
        when(userQueryService.getUserByUserId(USER_ID)).thenReturn(user(USER_ID));
        when(scheduleRepository.save(any(Schedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ScheduleResponse response = scheduleCommandService.createSchedule(
                USER_ID,
                new CreateScheduleRequest(
                        "치과",
                        false,
                        DATE,
                        LocalTime.of(14, 0),
                        DATE,
                        LocalTime.of(15, 0)
                )
        );

        assertThat(response.allDay()).isFalse();
        assertThat(response.startTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(response.endTime()).isEqualTo(LocalTime.of(15, 0));
    }

    @Test
    void createAllDayScheduleDiscardsTimes() {
        when(userQueryService.getUserByUserId(USER_ID)).thenReturn(user(USER_ID));
        when(scheduleRepository.save(any(Schedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ScheduleResponse response = scheduleCommandService.createSchedule(
                USER_ID,
                new CreateScheduleRequest(
                        "휴가",
                        true,
                        DATE,
                        LocalTime.of(14, 0),
                        DATE.plusDays(1),
                        LocalTime.of(15, 0)
                )
        );

        assertThat(response.allDay()).isTrue();
        assertThat(response.startTime()).isNull();
        assertThat(response.endTime()).isNull();
    }

    @Test
    void createScheduleReturnsSavedRecurrence() {
        RecurrenceRequest rule = new RecurrenceRequest(
                RecurrenceFreq.WEEKLY,
                1,
                "THU",
                RecurrenceEndType.NEVER,
                null,
                null
        );
        RecurrenceResponse savedRule = new RecurrenceResponse(
                RecurrenceFreq.WEEKLY,
                1,
                "THU",
                RecurrenceEndType.NEVER,
                null,
                null
        );
        when(userQueryService.getUserByUserId(USER_ID)).thenReturn(user(USER_ID));
        when(scheduleRepository.save(any(Schedule.class)))
                .thenReturn(timedSchedule(user(USER_ID)));
        when(recurrenceCommandService.upsert(
                RecurrenceOwnerType.SCHEDULE,
                SCHEDULE_ID,
                DATE,
                rule
        )).thenReturn(savedRule);

        ScheduleResponse response = scheduleCommandService.createSchedule(
                USER_ID,
                new CreateScheduleRequest(
                        "치과",
                        false,
                        DATE,
                        LocalTime.of(14, 0),
                        DATE,
                        LocalTime.of(15, 0),
                        rule
                )
        );

        assertThat(response.recurrence()).isEqualTo(savedRule);
        verify(recurrenceCommandService).upsert(
                RecurrenceOwnerType.SCHEDULE,
                SCHEDULE_ID,
                DATE,
                rule
        );
    }

    @Test
    void createTimedScheduleRequiresBothTimes() {
        when(userQueryService.getUserByUserId(USER_ID)).thenReturn(user(USER_ID));

        assertThatThrownBy(() -> scheduleCommandService.createSchedule(
                USER_ID,
                new CreateScheduleRequest("치과", false, DATE, null, DATE, LocalTime.NOON)
        ))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getErrorStatus())
                .isEqualTo(ErrorStatus.INVALID_SCHEDULE_PERIOD);
    }

    @Test
    void createScheduleRejectsEndBeforeStart() {
        when(userQueryService.getUserByUserId(USER_ID)).thenReturn(user(USER_ID));

        assertThatThrownBy(() -> scheduleCommandService.createSchedule(
                USER_ID,
                new CreateScheduleRequest(
                        "역순 일정",
                        false,
                        DATE,
                        LocalTime.of(15, 0),
                        DATE,
                        LocalTime.of(14, 0)
                )
        ))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getErrorStatus())
                .isEqualTo(ErrorStatus.INVALID_SCHEDULE_PERIOD);
    }

    @Test
    void updateScheduleChangesOnlyProvidedFields() {
        Schedule schedule = timedSchedule(user(USER_ID));
        when(scheduleQueryService.getOwnedSchedule(USER_ID, SCHEDULE_ID)).thenReturn(schedule);

        ScheduleResponse response = scheduleCommandService.updateSchedule(
                USER_ID,
                SCHEDULE_ID,
                new UpdateScheduleRequest("병원", null, null, null, null, null)
        );

        assertThat(response.title()).isEqualTo("병원");
        assertThat(response.startDate()).isEqualTo(DATE);
        assertThat(response.startTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(response.endTime()).isEqualTo(LocalTime.of(15, 0));
    }

    @Test
    void updateAllDayScheduleToTimedRequiresTimes() {
        Schedule schedule = Schedule.builder()
                .scheduleId(SCHEDULE_ID)
                .user(user(USER_ID))
                .title("휴가")
                .allDay(true)
                .startDate(DATE)
                .endDate(DATE)
                .build();
        when(scheduleQueryService.getOwnedSchedule(USER_ID, SCHEDULE_ID)).thenReturn(schedule);

        assertThatThrownBy(() -> scheduleCommandService.updateSchedule(
                USER_ID,
                SCHEDULE_ID,
                new UpdateScheduleRequest(null, false, null, null, null, null)
        ))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getErrorStatus())
                .isEqualTo(ErrorStatus.INVALID_SCHEDULE_PERIOD);
    }

    // 소유권/삭제 여부 검사는 ScheduleQueryService.getOwnedSchedule 한 곳에만 있으므로
    // 그쪽 테스트(ScheduleQueryServiceTest)에서만 검증한다.

    @Test
    void deleteScheduleSoftDeletesSchedule() {
        Schedule schedule = timedSchedule(user(USER_ID));
        when(scheduleQueryService.getOwnedSchedule(USER_ID, SCHEDULE_ID)).thenReturn(schedule);

        scheduleCommandService.deleteSchedule(USER_ID, SCHEDULE_ID);

        assertThat(schedule.deleted()).isTrue();
        verify(recurrenceCommandService).delete(RecurrenceOwnerType.SCHEDULE, SCHEDULE_ID);
    }

    private Schedule timedSchedule(User user) {
        return Schedule.builder()
                .scheduleId(SCHEDULE_ID)
                .user(user)
                .title("치과")
                .allDay(false)
                .startDate(DATE)
                .startTime(LocalTime.of(14, 0))
                .endDate(DATE)
                .endTime(LocalTime.of(15, 0))
                .build();
    }

    private User user(Long userId) {
        return User.builder()
                .userId(userId)
                .nickname("사용자")
                .build();
    }
}
