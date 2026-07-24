package yooze.withme.domain.group.dto.request;

import jakarta.validation.constraints.NotNull;
import yooze.withme.domain.group.enums.AttendanceStatus;

public record SubmitGroupResponseRequest(
        @NotNull(message = "참석 여부는 필수입니다.")
        AttendanceStatus attendanceStatus,

        String absenceReason
) {
}
