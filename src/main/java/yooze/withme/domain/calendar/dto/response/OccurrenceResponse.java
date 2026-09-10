package yooze.withme.domain.calendar.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import yooze.withme.domain.calendar.entity.RecurrenceException;

/**
 * 반복 전개 결과 한 회차. {@code occurrenceDate}는 규칙이 만들어낸 원본 날짜로 회차 식별자이고,
 * {@code date}는 예외로 옮겨졌을 수 있는 실제 표시 날짜다.
 * 나머지 필드가 null이면 원본(일정/Todo)의 값을 그대로 쓴다는 뜻이다.
 */
public record OccurrenceResponse(
        LocalDate occurrenceDate,
        LocalDate date,
        String title,
        LocalTime startTime,
        LocalTime endTime,
        Boolean completed
) {

    public static OccurrenceResponse of(LocalDate occurrenceDate) {
        return new OccurrenceResponse(occurrenceDate, occurrenceDate, null, null, null, null);
    }

    public static OccurrenceResponse from(RecurrenceException exception) {
        LocalDate occurrenceDate = exception.getOccurrenceDate();
        return new OccurrenceResponse(
                occurrenceDate,
                exception.getOverrideDate() == null ? occurrenceDate : exception.getOverrideDate(),
                exception.getOverrideTitle(),
                exception.getOverrideStartTime(),
                exception.getOverrideEndTime(),
                exception.getOverrideCompleted()
        );
    }
}
