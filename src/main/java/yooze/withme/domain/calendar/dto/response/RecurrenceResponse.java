package yooze.withme.domain.calendar.dto.response;

import java.time.LocalDate;
import yooze.withme.domain.calendar.entity.Recurrence;
import yooze.withme.domain.calendar.enums.RecurrenceEndType;
import yooze.withme.domain.calendar.enums.RecurrenceFreq;

public record RecurrenceResponse(
        RecurrenceFreq freq,
        int repeatInterval,
        String byDays,
        RecurrenceEndType endType,
        LocalDate endDate,
        Integer endCount
) {

    public static RecurrenceResponse from(Recurrence recurrence) {
        return new RecurrenceResponse(
                recurrence.getFreq(),
                recurrence.getRepeatInterval(),
                recurrence.getByDays(),
                recurrence.getEndType(),
                recurrence.getEndDate(),
                recurrence.getEndCount()
        );
    }
}
