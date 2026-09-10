package yooze.withme.domain.calendar.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import yooze.withme.domain.calendar.entity.Schedule;

public record ScheduleResponse(
        Long scheduleId,
        String title,
        boolean allDay,
        LocalDate startDate,
        LocalTime startTime,
        LocalDate endDate,
        LocalTime endTime,
        RecurrenceResponse recurrence
) {

    public static ScheduleResponse from(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getScheduleId(),
                schedule.getTitle(),
                schedule.isAllDay(),
                schedule.getStartDate(),
                schedule.getStartTime(),
                schedule.getEndDate(),
                schedule.getEndTime(),
                null
        );
    }

    public static ScheduleResponse from(
            Schedule schedule,
            RecurrenceResponse recurrence
    ) {
        return new ScheduleResponse(
                schedule.getScheduleId(),
                schedule.getTitle(),
                schedule.isAllDay(),
                schedule.getStartDate(),
                schedule.getStartTime(),
                schedule.getEndDate(),
                schedule.getEndTime(),
                recurrence
        );
    }
}
