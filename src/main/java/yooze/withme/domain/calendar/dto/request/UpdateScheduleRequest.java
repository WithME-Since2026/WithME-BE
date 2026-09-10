package yooze.withme.domain.calendar.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
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
        LocalTime endTime,

        @Schema(description = "null: 기존 반복 유지, false: 반복 해제, true: recurrence 규칙 적용")
        Boolean recurring,

        @Valid
        RecurrenceRequest recurrence
) {

    public UpdateScheduleRequest {
        title = title == null ? null : title.strip();
    }

    public UpdateScheduleRequest(
            String title,
            Boolean allDay,
            LocalDate startDate,
            LocalTime startTime,
            LocalDate endDate,
            LocalTime endTime
    ) {
        this(title, allDay, startDate, startTime, endDate, endTime, null, null);
    }

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "recurring이 true이면 반복 규칙이 필요하고, false이면 반복 규칙을 보낼 수 없습니다.")
    public boolean isRecurrenceChangeValid() {
        if (recurring == null) {
            return recurrence == null;
        }
        return recurring == (recurrence != null);
    }
}
