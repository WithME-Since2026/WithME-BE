package yooze.withme.domain.calendar.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.calendar.dto.response.ScheduleResponse;
import yooze.withme.domain.calendar.entity.Schedule;
import yooze.withme.domain.calendar.repository.ScheduleRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleQueryService {

    private final ScheduleRepository scheduleRepository;

    /** 본인 소유의 삭제되지 않은 개인 일정 상세를 조회한다. */
    public ScheduleResponse getSchedule(Long userId, Long scheduleId) {
        return ScheduleResponse.from(getOwnedSchedule(userId, scheduleId));
    }

    /**
     * 일정 접근 권한 검사의 단일 지점. 삭제된 일정은 존재하지 않는 것으로 취급한다.
     * 수정/삭제 경로(ScheduleCommandService)도 이 메서드를 거치므로 검사 규칙이 한 곳에만 존재한다.
     */
    public Schedule getOwnedSchedule(Long userId, Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.SCHEDULE_NOT_FOUND));
        if (schedule.deleted()) {
            throw new GeneralException(ErrorStatus.SCHEDULE_NOT_FOUND);
        }
        if (!schedule.getUser().getUserId().equals(userId)) {
            throw new GeneralException(ErrorStatus.SCHEDULE_FORBIDDEN);
        }
        return schedule;
    }
}
