package yooze.withme.domain.group.dto.response;

import yooze.withme.domain.group.entity.GroupResponse;
import yooze.withme.domain.group.enums.AttendanceStatus;

public record AttendanceResponse(
        Long memberId,
        Long userId,
        AttendanceStatus attendanceStatus,
        String absenceReason
) {

    public static AttendanceResponse from(GroupResponse response) {
        return new AttendanceResponse(
                response.getMember().getId(),
                response.getMember().getUserId(),
                response.getAttendanceStatus(),
                response.getAbsenceReason()
        );
    }
}
