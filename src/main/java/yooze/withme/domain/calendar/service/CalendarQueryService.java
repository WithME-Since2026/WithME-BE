package yooze.withme.domain.calendar.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.calendar.dto.response.CalendarItemResponse;
import yooze.withme.domain.calendar.dto.response.OccurrenceResponse;
import yooze.withme.domain.calendar.entity.Schedule;
import yooze.withme.domain.calendar.enums.RecurrenceOwnerType;
import yooze.withme.domain.calendar.repository.HolidayRepository;
import yooze.withme.domain.calendar.repository.ScheduleRepository;
import yooze.withme.domain.group.entity.GroupMember;
import yooze.withme.domain.group.entity.GroupResponse;
import yooze.withme.domain.group.entity.GroupRound;
import yooze.withme.domain.group.enums.AttendanceStatus;
import yooze.withme.domain.group.enums.GroupMemberStatus;
import yooze.withme.domain.group.repository.GroupMemberRepository;
import yooze.withme.domain.group.repository.GroupResponseRepository;
import yooze.withme.domain.group.repository.GroupRoundRepository;
import yooze.withme.domain.todo.entity.Todo;
import yooze.withme.domain.todo.repository.TodoRepository;

/**
 * 캘린더 화면 한 번에 필요한 4개 소스를 합쳐 평탄한 배열로 돌려준다.
 * 컨벤션상 서비스는 리포지터리 하나만 보지만, 캘린더는 성격상 여러 소스를 봐야 하므로
 * 원본 소스는 리포지터리로 직접 읽고 반복 전개와 공휴일만 각 QueryService 에 위임한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarQueryService {

    /** 한 번에 조회 가능한 최대 기간. 반복 전개량이 구간 일수로 자연 제한된다. */
    public static final int MAX_RANGE_DAYS = 92;

    private final TodoRepository todoRepository;
    private final ScheduleRepository scheduleRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupRoundRepository groupRoundRepository;
    private final GroupResponseRepository groupResponseRepository;
    private final HolidayRepository holidayRepository;
    private final RecurrenceQueryService recurrenceQueryService;

    /** 기간 안의 내 todo·개인 일정·모임 회차·공휴일을 한 배열로 돌려준다. */
    public List<CalendarItemResponse> getCalendar(Long userId, LocalDate from, LocalDate to) {
        validateRange(from, to);

        List<CalendarItemResponse> items = new ArrayList<>();
        items.addAll(todoItems(userId, from, to));
        items.addAll(scheduleItems(userId, from, to));
        items.addAll(groupRoundItems(userId, from, to));
        items.addAll(holidayItems(from, to));

        // 종일 일정을 시간 일정보다 앞에 두어 클라이언트가 그대로 그리면 되게 한다
        items.sort(Comparator.comparing(CalendarItemResponse::date)
                .thenComparing(item -> item.startTime() == null ? LocalTime.MIN : item.startTime())
                .thenComparing(CalendarItemResponse::sourceType));
        return items;
    }

    private List<CalendarItemResponse> todoItems(Long userId, LocalDate from, LocalDate to) {
        List<CalendarItemResponse> items = new ArrayList<>();
        List<Todo> todos = todoRepository.findForCalendar(userId, from, to);
        Set<Long> recurringIds = recurrenceQueryService.findAll(
                RecurrenceOwnerType.TODO, todos.stream().map(Todo::getTodoId).toList()).keySet();

        for (Todo todo : todos) {
            // ponytail: 반복 원본 1건당 규칙+예외 조회 1번. 구간이 최대 92일이라 반복 원본 수가
            // 곧 상한이다. 반복 todo 가 수백 건이 되면 규칙/예외를 일괄 조회로 바꿀 것.
            List<OccurrenceResponse> occurrences = recurrenceQueryService.findOccurrences(
                    RecurrenceOwnerType.TODO, todo.getTodoId(), todo.getDueDate(), from, to);

            if (occurrences.isEmpty()) {
                // 반복인데 전개 결과가 비면 구간 안에 회차가 없는 것이므로 원본으로 대체하지 않는다
                if (!recurringIds.contains(todo.getTodoId())) {
                    addIfWithin(items, CalendarItemResponse.ofTodo(todo, null), from, to);
                }
                continue;
            }
            for (OccurrenceResponse occurrence : occurrences) {
                items.add(CalendarItemResponse.ofTodo(todo, occurrence));
            }
        }
        return items;
    }

    private List<CalendarItemResponse> scheduleItems(Long userId, LocalDate from, LocalDate to) {
        List<CalendarItemResponse> items = new ArrayList<>();
        List<Schedule> schedules = scheduleRepository.findForCalendar(userId, from, to);
        Set<Long> recurringIds = recurrenceQueryService.findAll(
                RecurrenceOwnerType.SCHEDULE,
                schedules.stream().map(Schedule::getScheduleId).toList()).keySet();

        for (Schedule schedule : schedules) {
            List<OccurrenceResponse> occurrences = recurrenceQueryService.findOccurrences(
                    RecurrenceOwnerType.SCHEDULE, schedule.getScheduleId(),
                    schedule.getStartDate(), from, to);

            if (occurrences.isEmpty()) {
                // 반복이면 구간 안에 회차가 없는 것(규칙 종료·전수 건너뜀)이므로 제외하고,
                // 비반복 다일 일정만 시작일이 구간 앞이어도 한 칸으로 내려준다
                if (!recurringIds.contains(schedule.getScheduleId())) {
                    items.add(CalendarItemResponse.ofSchedule(schedule, null));
                }
                continue;
            }
            for (OccurrenceResponse occurrence : occurrences) {
                items.add(CalendarItemResponse.ofSchedule(schedule, occurrence));
            }
        }
        return items;
    }

    private List<CalendarItemResponse> groupRoundItems(Long userId, LocalDate from, LocalDate to) {
        List<Long> groupIds = groupMemberRepository
                .findByUserIdAndStatus(userId, GroupMemberStatus.ACTIVE).stream()
                .map(member -> member.getGroup().getId())
                .toList();
        if (groupIds.isEmpty()) {
            return List.of();
        }

        List<GroupRound> rounds =
                groupRoundRepository.findByGroupIdsAndRoundDateBetween(groupIds, from, to);
        if (rounds.isEmpty()) {
            return List.of();
        }

        Map<Long, AttendanceStatus> myStatus = groupResponseRepository
                .findByUserIdAndGroupRoundIdIn(userId, rounds.stream().map(GroupRound::getId).toList())
                .stream()
                .collect(Collectors.toMap(
                        response -> response.getGroupRound().getId(),
                        GroupResponse::getAttendanceStatus,
                        (first, second) -> first
                ));

        return rounds.stream()
                // 아직 응답하지 않은 회차는 PENDING 으로 본다
                .map(round -> CalendarItemResponse.ofGroupRound(
                        round,
                        myStatus.getOrDefault(round.getId(), AttendanceStatus.PENDING)))
                .toList();
    }

    private List<CalendarItemResponse> holidayItems(LocalDate from, LocalDate to) {
        return holidayRepository.findByDateBetweenOrderByDateAsc(from, to).stream()
                .map(CalendarItemResponse::ofHoliday)
                .toList();
    }

    private void addIfWithin(
            List<CalendarItemResponse> items,
            CalendarItemResponse item,
            LocalDate from,
            LocalDate to
    ) {
        if (!item.date().isBefore(from) && !item.date().isAfter(to)) {
            items.add(item);
        }
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)
                || ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new GeneralException(ErrorStatus.CALENDAR_RANGE_TOO_WIDE);
        }
    }
}
