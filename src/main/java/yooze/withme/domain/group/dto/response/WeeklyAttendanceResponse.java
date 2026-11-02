package yooze.withme.domain.group.dto.response;

import java.time.LocalDate;

public record WeeklyAttendanceResponse(
        LocalDate weekStartDate,
        long attendCount,
        long absentCount,
        double attendanceRate
) {

    /** 비율 계산은 전체 참여율(AttendanceRateResponse)과 같은 식을 쓴다 */
    public static WeeklyAttendanceResponse of(LocalDate weekStartDate, long attendCount, long absentCount) {
        AttendanceRateResponse rate = AttendanceRateResponse.of(attendCount, absentCount);
        return new WeeklyAttendanceResponse(weekStartDate, attendCount, absentCount, rate.attendanceRate());
    }
}
