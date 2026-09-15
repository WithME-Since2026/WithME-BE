package yooze.withme.domain.calendar.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Locale;
import yooze.withme.domain.calendar.enums.RecurrenceEndType;
import yooze.withme.domain.calendar.enums.RecurrenceFreq;

public record RecurrenceRequest(
        @NotNull(message = "반복 주기는 필수입니다.")
        RecurrenceFreq freq,

        @Min(value = 1, message = "반복 간격은 1 이상이어야 합니다.")
        int repeatInterval,

        String byDays,

        @NotNull(message = "반복 종료 조건은 필수입니다.")
        RecurrenceEndType endType,

        LocalDate endDate,

        @Min(value = 1, message = "반복 횟수는 1 이상이어야 합니다.")
        @Max(value = 1000, message = "반복 횟수는 1000 이하여야 합니다.")
        Integer endCount
) {

    public RecurrenceRequest {
        byDays = byDays == null
                ? null
                : byDays.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
    }
}
