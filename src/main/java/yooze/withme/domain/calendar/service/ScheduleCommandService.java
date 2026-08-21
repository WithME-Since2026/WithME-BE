package yooze.withme.domain.calendar.service;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.auth.service.UserQueryService;
import yooze.withme.domain.calendar.dto.request.CreateScheduleRequest;
import yooze.withme.domain.calendar.dto.request.UpdateScheduleRequest;
import yooze.withme.domain.calendar.dto.response.RecurrenceResponse;
import yooze.withme.domain.calendar.dto.response.ScheduleResponse;
import yooze.withme.domain.calendar.entity.Schedule;
import yooze.withme.domain.calendar.enums.RecurrenceOwnerType;
import yooze.withme.domain.calendar.repository.ScheduleRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleCommandService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleQueryService scheduleQueryService;
    private final RecurrenceCommandService recurrenceCommandService;
    private final UserQueryService userQueryService;

    /** 개인 일정과 선택적인 반복 규칙을 함께 생성한다. */
    public ScheduleResponse createSchedule(Long userId, CreateScheduleRequest request) {
        User user = userQueryService.getUserByUserId(userId);
        boolean allDay = request.allDay();
        LocalTime startTime = allDay ? null : request.startTime();
        LocalTime endTime = allDay ? null : request.endTime();
        validatePeriod(allDay, request.startDate(), startTime, request.endDate(), endTime);

        Schedule schedule = scheduleRepository.save(Schedule.builder()
                .user(user)
                .title(request.title())
                .allDay(allDay)
                .startDate(request.startDate())
                .startTime(startTime)
                .endDate(request.endDate())
                .endTime(endTime)
                .build());

        RecurrenceResponse recurrence = request.recurrence() == null
                ? null
                : recurrenceCommandService.upsert(
                        RecurrenceOwnerType.SCHEDULE,
                        schedule.getScheduleId(),
                        schedule.getStartDate(),
                        request.recurrence()
                );
        return ScheduleResponse.from(schedule, recurrence);
    }

    /** 전달된 필드만 변경하고 병합된 전체 기간을 다시 검증한다. */
    public ScheduleResponse updateSchedule(
            Long userId,
            Long scheduleId,
            UpdateScheduleRequest request
    ) {
        Schedule schedule = scheduleQueryService.getOwnedSchedule(userId, scheduleId);

        String title = schedule.getTitle();
        if (request.title() != null) {
            title = request.title();
        }

        boolean allDay = schedule.isAllDay();
        if (request.allDay() != null) {
            allDay = request.allDay();
        }

        LocalDate startDate = schedule.getStartDate();
        if (request.startDate() != null) {
            startDate = request.startDate();
        }

        LocalDate endDate = schedule.getEndDate();
        if (request.endDate() != null) {
            endDate = request.endDate();
        }

        // 종일 일정으로 바뀌면 기존에 저장돼 있던 시각은 버린다
        LocalTime startTime = null;
        LocalTime endTime = null;
        if (!allDay) {
            startTime = schedule.getStartTime();
            if (request.startTime() != null) {
                startTime = request.startTime();
            }
            endTime = schedule.getEndTime();
            if (request.endTime() != null) {
                endTime = request.endTime();
            }
        }

        validatePeriod(allDay, startDate, startTime, endDate, endTime);
        schedule.update(title, allDay, startDate, startTime, endDate, endTime);

        RecurrenceResponse recurrence = request.recurring() == null
                ? recurrenceCommandService.validateExisting(
                        RecurrenceOwnerType.SCHEDULE,
                        scheduleId,
                        startDate
                )
                : updateRecurrence(scheduleId, startDate, request);
        return ScheduleResponse.from(schedule, recurrence);
    }

    /** 본인 소유 일정을 소프트 삭제한다. */
    public void deleteSchedule(Long userId, Long scheduleId) {
        Schedule schedule = scheduleQueryService.getOwnedSchedule(userId, scheduleId);
        recurrenceCommandService.delete(RecurrenceOwnerType.SCHEDULE, scheduleId);
        schedule.delete();
    }

    private RecurrenceResponse updateRecurrence(
            Long scheduleId,
            LocalDate anchorDate,
            UpdateScheduleRequest request
    ) {
        if (Boolean.FALSE.equals(request.recurring())) {
            recurrenceCommandService.delete(RecurrenceOwnerType.SCHEDULE, scheduleId);
            return null;
        }
        return recurrenceCommandService.upsert(
                RecurrenceOwnerType.SCHEDULE,
                scheduleId,
                anchorDate,
                request.recurrence()
        );
    }

    private void validatePeriod(
            boolean allDay,
            LocalDate startDate,
            LocalTime startTime,
            LocalDate endDate,
            LocalTime endTime
    ) {
        if (endDate.isBefore(startDate)
                || (!allDay && (startTime == null || endTime == null))
                || (!allDay && startDate.equals(endDate) && endTime.isBefore(startTime))) {
            throw new GeneralException(ErrorStatus.INVALID_SCHEDULE_PERIOD);
        }
    }
}
