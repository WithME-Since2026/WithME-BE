package yooze.withme.domain.group.dto.response;

public record AttendanceRateResponse(
        long attendCount,
        long absentCount,
        double attendanceRate
) {

    /** 결정된 응답(ATTEND/ABSENT) 중 ATTEND 비율. 아직 아무 회차도 결정 안 됐으면 0.0 */
    public static AttendanceRateResponse of(long attendCount, long absentCount) {
        long resolvedCount = attendCount + absentCount;
        double attendanceRate;
        if (resolvedCount == 0) {
            attendanceRate = 0.0;
        } else {
            attendanceRate = (double) attendCount / resolvedCount;
        }
        return new AttendanceRateResponse(attendCount, absentCount, attendanceRate);
    }
}
