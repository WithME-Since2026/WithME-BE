package yooze.withme.domain.calendar.dto.response;

import java.time.LocalDate;
import yooze.withme.domain.calendar.entity.Holiday;
import yooze.withme.domain.calendar.enums.HolidayType;

public record HolidayResponse(
        LocalDate date,
        String name,
        HolidayType type,
        boolean restDay
) {

    public static HolidayResponse from(Holiday holiday) {
        return new HolidayResponse(
                holiday.getDate(),
                holiday.getName(),
                holiday.getType(),
                holiday.isRestDay()
        );
    }
}
