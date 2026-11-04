package yooze.withme.domain.group.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import yooze.withme.domain.group.dto.response.WeeklyAttendanceResponse;
import yooze.withme.domain.group.entity.GroupResponse;
import yooze.withme.domain.group.entity.GroupRound;
import yooze.withme.domain.group.enums.AttendanceStatus;

class GroupQueryServiceWeeklyTest {

    @Test
    void groupsResponsesByMondayWeekAndFillsEmptyWeeks() {
        LocalDate from = LocalDate.of(2026, 9, 21); // 월요일

        List<WeeklyAttendanceResponse> result = GroupQueryService.groupByWeek(from, 3, List.of(
                response(LocalDate.of(2026, 9, 21), AttendanceStatus.ATTEND),
                response(LocalDate.of(2026, 9, 27), AttendanceStatus.ATTEND), // 같은 주 일요일
                response(LocalDate.of(2026, 9, 24), AttendanceStatus.ABSENT),
                response(LocalDate.of(2026, 10, 5), AttendanceStatus.PENDING)  // 집계 제외
        ));

        assertThat(result).hasSize(3);
        assertThat(result.get(0)).isEqualTo(new WeeklyAttendanceResponse(from, 2, 1, 2.0 / 3));
        assertThat(result.get(1)).isEqualTo(new WeeklyAttendanceResponse(LocalDate.of(2026, 9, 28), 0, 0, 0.0));
        assertThat(result.get(2)).isEqualTo(new WeeklyAttendanceResponse(LocalDate.of(2026, 10, 5), 0, 0, 0.0));
    }

    private GroupResponse response(LocalDate roundDate, AttendanceStatus status) {
        return GroupResponse.builder()
                .groupRound(GroupRound.builder().roundDate(roundDate).build())
                .attendanceStatus(status)
                .build();
    }
}
