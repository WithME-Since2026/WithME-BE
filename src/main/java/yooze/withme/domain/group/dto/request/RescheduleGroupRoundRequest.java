package yooze.withme.domain.group.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record RescheduleGroupRoundRequest(
        @NotNull(message = "변경할 날짜는 필수입니다.")
        LocalDate roundDate,

        @NotNull(message = "변경할 시간은 필수입니다.")
        LocalTime roundTime,

        @Size(max = 127, message = "장소 이름은 127자를 넘을 수 없습니다.")
        String locationName,

        @Size(max = 255, message = "장소 주소는 255자를 넘을 수 없습니다.")
        String locationAddress
) {
}
