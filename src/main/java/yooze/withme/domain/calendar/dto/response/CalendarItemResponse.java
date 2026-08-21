package yooze.withme.domain.calendar.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.time.LocalTime;
import yooze.withme.domain.calendar.entity.Holiday;
import yooze.withme.domain.calendar.entity.Schedule;
import yooze.withme.domain.calendar.enums.CalendarSourceType;
import yooze.withme.domain.group.entity.GroupRound;
import yooze.withme.domain.group.enums.AttendanceStatus;
import yooze.withme.domain.todo.entity.Todo;

/**
 * 캘린더 한 칸. 소스마다 의미 없는 필드는 응답에서 빠진다.
 * 개별 회차를 수정할 때는 {@code sourceId} + {@code occurrenceDate} 두 값을 그대로 돌려보내면 된다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CalendarItemResponse(
        CalendarSourceType sourceType,
        Long sourceId,
        /** 규칙이 만들어낸 원본 날짜 = 회차 식별자. 반복이 아니면 표시 날짜와 같다. */
        LocalDate occurrenceDate,
        /** 실제로 화면에 찍히는 날짜. 회차가 옮겨졌으면 occurrenceDate 와 다르다. */
        LocalDate date,
        String title,
        boolean allDay,
        LocalTime startTime,
        LocalTime endTime,
        /** 여러 날에 걸친 개인 일정의 종료일. 하루짜리면 생략된다. */
        LocalDate endDate,
        Boolean recurring,
        Boolean completed,
        Long categoryId,
        Long groupId,
        AttendanceStatus attendanceStatus,
        String locationName,
        Boolean restDay
) {

    public static CalendarItemResponse ofTodo(Todo todo, OccurrenceResponse occurrence) {
        boolean recurring = occurrence != null;
        LocalDate occurrenceDate = recurring ? occurrence.occurrenceDate() : todo.getDueDate();
        return new CalendarItemResponse(
                CalendarSourceType.TODO,
                todo.getTodoId(),
                occurrenceDate,
                recurring ? occurrence.date() : todo.getDueDate(),
                recurring && occurrence.title() != null ? occurrence.title() : todo.getTitle(),
                true,
                null,
                null,
                null,
                recurring,
                // 회차별 완료가 기록돼 있으면 그 값이 원본 플래그를 이긴다
                recurring && occurrence.completed() != null
                        ? occurrence.completed()
                        : todo.isCompleted(),
                todo.getCategory() == null ? null : todo.getCategory().getCategoryId(),
                null,
                null,
                null,
                null
        );
    }

    public static CalendarItemResponse ofSchedule(Schedule schedule, OccurrenceResponse occurrence) {
        boolean recurring = occurrence != null;
        LocalDate date = recurring ? occurrence.date() : schedule.getStartDate();
        return new CalendarItemResponse(
                CalendarSourceType.SCHEDULE,
                schedule.getScheduleId(),
                recurring ? occurrence.occurrenceDate() : schedule.getStartDate(),
                date,
                recurring && occurrence.title() != null ? occurrence.title() : schedule.getTitle(),
                schedule.isAllDay(),
                startTimeOf(schedule, occurrence),
                endTimeOf(schedule, occurrence),
                // 반복 회차는 하루짜리로 전개한다. 여러 날에 걸치는 건 비반복 원본만.
                recurring || schedule.getEndDate().equals(date) ? null : schedule.getEndDate(),
                recurring,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static CalendarItemResponse ofGroupRound(
            GroupRound round,
            AttendanceStatus attendanceStatus
    ) {
        return new CalendarItemResponse(
                CalendarSourceType.GROUP_ROUND,
                round.getId(),
                round.getRoundDate(),
                round.getRoundDate(),
                round.getGroup().getName(),
                round.getRoundTime() == null,
                round.getRoundTime(),
                null,
                null,
                null,
                null,
                null,
                round.getGroup().getId(),
                attendanceStatus,
                round.getRoundLocationName(),
                null
        );
    }

    public static CalendarItemResponse ofHoliday(Holiday holiday) {
        return new CalendarItemResponse(
                CalendarSourceType.HOLIDAY,
                null,
                holiday.getDate(),
                holiday.getDate(),
                holiday.getName(),
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                holiday.isRestDay()
        );
    }

    private static LocalTime startTimeOf(Schedule schedule, OccurrenceResponse occurrence) {
        if (schedule.isAllDay()) {
            return null;
        }
        return occurrence != null && occurrence.startTime() != null
                ? occurrence.startTime()
                : schedule.getStartTime();
    }

    private static LocalTime endTimeOf(Schedule schedule, OccurrenceResponse occurrence) {
        if (schedule.isAllDay()) {
            return null;
        }
        return occurrence != null && occurrence.endTime() != null
                ? occurrence.endTime()
                : schedule.getEndTime();
    }
}
