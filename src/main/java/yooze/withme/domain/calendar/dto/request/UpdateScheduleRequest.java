package yooze.withme.domain.calendar.dto.request;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateScheduleRequest(
        @Size(min = 1, max = 255, message = "제목은 1자 이상 255자 이하여야 합니다.")
        String title,
        Boolean allDay,
        LocalDate startDate,
        LocalTime startTime,
        LocalDate endDate,
        LocalTime endTime
) {

    public UpdateScheduleRequest {
        title = title == null ? null : title.strip();
    }
}
