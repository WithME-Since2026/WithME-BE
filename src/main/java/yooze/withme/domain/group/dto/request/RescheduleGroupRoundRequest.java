package yooze.withme.domain.group.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record RescheduleGroupRoundRequest(
        @NotNull(message = "변경할 날짜는 필수입니다.")
        LocalDate roundDate,

        @NotNull(message = "변경할 시간은 필수입니다.")
        LocalTime roundTime,

        String locationName,

        String locationAddress
) {
}
