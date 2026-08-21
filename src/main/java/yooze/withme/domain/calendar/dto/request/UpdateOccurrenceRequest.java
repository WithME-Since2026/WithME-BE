package yooze.withme.domain.calendar.dto.request;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

/** 반복 일정의 특정 회차만 덮어쓰는 요청. 전달된 필드만 원본 값을 대체한다. */
public record UpdateOccurrenceRequest(
        @Size(max = 255, message = "제목은 255자를 초과할 수 없습니다.")
        String title,

        LocalDate date,

        LocalTime startTime,

        LocalTime endTime,

        Boolean completed
) {

    public UpdateOccurrenceRequest {
        title = title == null ? null : title.strip();
    }

    public boolean isEmpty() {
        return (title == null || title.isBlank())
                && date == null
                && startTime == null
                && endTime == null
                && completed == null;
    }
}
