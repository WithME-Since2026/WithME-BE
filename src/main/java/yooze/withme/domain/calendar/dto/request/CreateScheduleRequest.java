package yooze.withme.domain.calendar.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateScheduleRequest(
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 255, message = "제목은 255자를 초과할 수 없습니다.")
        String title,

        boolean allDay,

        @NotNull(message = "시작일은 필수입니다.")
        LocalDate startDate,

        LocalTime startTime,

        @NotNull(message = "종료일은 필수입니다.")
        LocalDate endDate,

        LocalTime endTime
) {

    public CreateScheduleRequest {
        title = title == null ? null : title.strip();
    }
}
