package yooze.withme.domain.calendar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.calendar.dto.response.CalendarItemResponse;
import yooze.withme.domain.calendar.dto.response.OccurrenceResponse;
import yooze.withme.domain.calendar.dto.response.RecurrenceResponse;
import yooze.withme.domain.calendar.entity.Holiday;
import yooze.withme.domain.calendar.entity.Schedule;
import yooze.withme.domain.calendar.enums.CalendarSourceType;
import yooze.withme.domain.calendar.enums.HolidayType;
import yooze.withme.domain.calendar.enums.RecurrenceEndType;
import yooze.withme.domain.calendar.enums.RecurrenceFreq;
import yooze.withme.domain.calendar.enums.RecurrenceOwnerType;
import yooze.withme.domain.calendar.repository.HolidayRepository;
import yooze.withme.domain.calendar.repository.ScheduleRepository;
import yooze.withme.domain.group.repository.GroupMemberRepository;
import yooze.withme.domain.group.repository.GroupResponseRepository;
import yooze.withme.domain.group.repository.GroupRoundRepository;
import yooze.withme.domain.todo.entity.Todo;
import yooze.withme.domain.todo.repository.TodoRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CalendarQueryServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate FROM = LocalDate.of(2026, 8, 1);
    private static final LocalDate TO = LocalDate.of(2026, 8, 31);

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private GroupRoundRepository groupRoundRepository;

    @Mock
    private GroupResponseRepository groupResponseRepository;

    @Mock
    private HolidayRepository holidayRepository;

    @Mock
    private RecurrenceQueryService recurrenceQueryService;

    private CalendarQueryService calendarQueryService;

    @BeforeEach
    void setUp() {
        calendarQueryService = new CalendarQueryService(
                todoRepository,
                scheduleRepository,
                groupMemberRepository,
                groupRoundRepository,
                groupResponseRepository,
                holidayRepository,
                recurrenceQueryService
        );
        when(todoRepository.findForCalendar(any(), any(), any())).thenReturn(List.of());
        when(scheduleRepository.findForCalendar(any(), any(), any())).thenReturn(List.of());
        when(groupMemberRepository.findByUserIdAndStatus(any(), any())).thenReturn(List.of());
        when(holidayRepository.findByDateBetweenOrderByDateAsc(any(), any())).thenReturn(List.of());
        when(recurrenceQueryService.findOccurrences(any(), anyLong(), any(), any(), any()))
                .thenReturn(List.of());
    }

    @Test
    void rejectsRangeWiderThanLimit() {
        // 양 끝을 포함하므로 간격이 92일이면 93일치가 되어 상한을 넘는다
        assertThatThrownBy(() -> calendarQueryService.getCalendar(
                USER_ID, FROM, FROM.plusDays(CalendarQueryService.MAX_RANGE_DAYS)))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getErrorStatus())
                .isEqualTo(ErrorStatus.CALENDAR_RANGE_TOO_WIDE);
    }

    @Test
    void allowsExactlyMaxRangeDays() {
        assertThat(calendarQueryService.getCalendar(
                USER_ID, FROM, FROM.plusDays(CalendarQueryService.MAX_RANGE_DAYS - 1))).isEmpty();
    }

    @Test
    void rejectsReversedRange() {
        assertThatThrownBy(() -> calendarQueryService.getCalendar(USER_ID, TO, FROM))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getErrorStatus())
                .isEqualTo(ErrorStatus.CALENDAR_RANGE_TOO_WIDE);
    }

    @Test
    void sortsByDateAndPutsAllDayItemsFirst() {
        when(holidayRepository.findByDateBetweenOrderByDateAsc(FROM, TO))
                .thenReturn(List.of(holiday(LocalDate.of(2026, 8, 15))));
        when(scheduleRepository.findForCalendar(USER_ID, FROM, TO))
                .thenReturn(List.of(timedSchedule(LocalDate.of(2026, 8, 15))));
        when(todoRepository.findForCalendar(USER_ID, FROM, TO))
                .thenReturn(List.of(todo(LocalDate.of(2026, 8, 10))));

        List<CalendarItemResponse> items = calendarQueryService.getCalendar(USER_ID, FROM, TO);

        assertThat(items).extracting(CalendarItemResponse::sourceType).containsExactly(
                CalendarSourceType.TODO,        // 8/10
                CalendarSourceType.HOLIDAY,     // 8/15 종일
                CalendarSourceType.SCHEDULE     // 8/15 14:00
        );
    }

    @Test
    void expandsRecurringTodoAndLetsOccurrenceOverrideCompletion() {
        Todo todo = todo(LocalDate.of(2026, 8, 3));
        when(todoRepository.findForCalendar(USER_ID, FROM, TO)).thenReturn(List.of(todo));
        when(recurrenceQueryService.findOccurrences(
                RecurrenceOwnerType.TODO, todo.getTodoId(), todo.getDueDate(), FROM, TO))
                .thenReturn(List.of(
                        OccurrenceResponse.of(LocalDate.of(2026, 8, 3)),
                        new OccurrenceResponse(
                                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 10),
                                null, null, null, true)
                ));

        List<CalendarItemResponse> items = calendarQueryService.getCalendar(USER_ID, FROM, TO);

        assertThat(items).hasSize(2);
        assertThat(items).allMatch(CalendarItemResponse::recurring);
        assertThat(items.get(0).completed()).isFalse();
        assertThat(items.get(1).completed()).isTrue();
    }

    @Test
    void keepsNonRecurringTodoOutsideRangeOut() {
        // 반복 규칙이 붙었다가 사라진 원본은 마감일이 구간 밖일 수 있다
        when(todoRepository.findForCalendar(USER_ID, FROM, TO))
                .thenReturn(List.of(todo(LocalDate.of(2026, 7, 20))));

        assertThat(calendarQueryService.getCalendar(USER_ID, FROM, TO)).isEmpty();
    }

    @Test
    void keepsRecurringScheduleWithNoOccurrenceOut() {
        // 규칙이 끝났거나 회차가 전부 건너뛰어진 반복 일정은 원본으로 대체하지 않는다
        Schedule schedule = timedSchedule(LocalDate.of(2026, 7, 20));
        when(scheduleRepository.findForCalendar(USER_ID, FROM, TO)).thenReturn(List.of(schedule));
        when(recurrenceQueryService.findAll(RecurrenceOwnerType.SCHEDULE,
                List.of(schedule.getScheduleId())))
                .thenReturn(Map.of(schedule.getScheduleId(), new RecurrenceResponse(
                        RecurrenceFreq.WEEKLY, 1, "MO", RecurrenceEndType.DATE,
                        LocalDate.of(2026, 7, 31), null)));

        assertThat(calendarQueryService.getCalendar(USER_ID, FROM, TO)).isEmpty();
    }

    @Test
    void keepsNonRecurringMultiDayScheduleStartingBeforeRange() {
        when(scheduleRepository.findForCalendar(USER_ID, FROM, TO))
                .thenReturn(List.of(timedSchedule(LocalDate.of(2026, 7, 20))));

        assertThat(calendarQueryService.getCalendar(USER_ID, FROM, TO)).hasSize(1);
    }

    @Test
    void keepsRecurringTodoWithAllOccurrencesSkippedOut() {
        // 구간 안 유일한 회차를 SKIP 하면 전개 결과가 비지만 원본 마감일로 되살아나면 안 된다
        Todo todo = todo(LocalDate.of(2026, 8, 3));
        when(todoRepository.findForCalendar(USER_ID, FROM, TO)).thenReturn(List.of(todo));
        when(recurrenceQueryService.findAll(RecurrenceOwnerType.TODO, List.of(todo.getTodoId())))
                .thenReturn(Map.of(todo.getTodoId(), new RecurrenceResponse(
                        RecurrenceFreq.WEEKLY, 1, "MON", RecurrenceEndType.NEVER, null, null)));

        assertThat(calendarQueryService.getCalendar(USER_ID, FROM, TO)).isEmpty();
    }

    private Todo todo(LocalDate dueDate) {
        return Todo.builder()
                .todoId(7L)
                .user(User.builder().userId(USER_ID).nickname("사용자").build())
                .title("주간 회고")
                .dueDate(dueDate)
                .build();
    }

    private Schedule timedSchedule(LocalDate date) {
        return Schedule.builder()
                .scheduleId(9L)
                .user(User.builder().userId(USER_ID).nickname("사용자").build())
                .title("치과")
                .allDay(false)
                .startDate(date)
                .startTime(LocalTime.of(14, 0))
                .endDate(date)
                .endTime(LocalTime.of(15, 0))
                .build();
    }

    private Holiday holiday(LocalDate date) {
        return Holiday.builder()
                .date(date)
                .name("광복절")
                .type(HolidayType.HOLIDAY)
                .restDay(true)
                .build();
    }
}
