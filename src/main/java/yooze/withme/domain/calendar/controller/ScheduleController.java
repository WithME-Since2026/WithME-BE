package yooze.withme.domain.calendar.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.common.status.SuccessStatus;
import yooze.withme.domain.calendar.controller.docs.ScheduleControllerDocs;
import yooze.withme.domain.calendar.dto.request.CreateScheduleRequest;
import yooze.withme.domain.calendar.dto.request.UpdateScheduleRequest;
import yooze.withme.domain.calendar.dto.response.ScheduleResponse;
import yooze.withme.domain.calendar.service.ScheduleCommandService;
import yooze.withme.domain.calendar.service.ScheduleQueryService;

@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class ScheduleController implements ScheduleControllerDocs {

    private final ScheduleCommandService scheduleCommandService;
    private final ScheduleQueryService scheduleQueryService;

    @Override
    public ResponseEntity<ApiResponse<ScheduleResponse>> createSchedule(
            @RequestAttribute("userId") Long userId,
            @Valid @RequestBody CreateScheduleRequest request
    ) {
        ScheduleResponse response = scheduleCommandService.createSchedule(userId, request);
        return ApiResponse.success(SuccessStatus.CREATE_SCHEDULE_SUCCESS, response);
    }

    @Override
    public ResponseEntity<ApiResponse<ScheduleResponse>> getSchedule(
            @RequestAttribute("userId") Long userId,
            Long scheduleId
    ) {
        ScheduleResponse response = scheduleQueryService.getSchedule(userId, scheduleId);
        return ApiResponse.success(SuccessStatus.GET_SCHEDULE_SUCCESS, response);
    }

    @Override
    public ResponseEntity<ApiResponse<ScheduleResponse>> updateSchedule(
            @RequestAttribute("userId") Long userId,
            Long scheduleId,
            @Valid @RequestBody UpdateScheduleRequest request
    ) {
        ScheduleResponse response = scheduleCommandService.updateSchedule(userId, scheduleId, request);
        return ApiResponse.success(SuccessStatus.UPDATE_SCHEDULE_SUCCESS, response);
    }

    @Override
    public ResponseEntity<ApiResponse<Void>> deleteSchedule(
            @RequestAttribute("userId") Long userId,
            Long scheduleId
    ) {
        scheduleCommandService.deleteSchedule(userId, scheduleId);
        return ApiResponse.success(SuccessStatus.DELETE_SCHEDULE_SUCCESS);
    }
}
