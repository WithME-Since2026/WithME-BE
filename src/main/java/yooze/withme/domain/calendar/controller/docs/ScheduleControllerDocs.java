package yooze.withme.domain.calendar.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.domain.calendar.dto.request.CreateScheduleRequest;
import yooze.withme.domain.calendar.dto.request.UpdateOccurrenceRequest;
import yooze.withme.domain.calendar.dto.request.UpdateScheduleRequest;
import yooze.withme.domain.calendar.dto.response.OccurrenceResponse;
import yooze.withme.domain.calendar.dto.response.ScheduleResponse;

@Tag(name = "Schedule", description = "개인 일정 생성/조회/수정/삭제 API")
@SecurityRequirement(name = "bearerAuth")
public interface ScheduleControllerDocs {

    @Operation(summary = "개인 일정 생성", description = "recurrence를 보내면 반복 일정으로 생성합니다.")
    @PostMapping
    ResponseEntity<ApiResponse<ScheduleResponse>> createSchedule(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @Valid @RequestBody CreateScheduleRequest request
    );

    @Operation(summary = "개인 일정 상세 조회", description = "반복 일정이면 recurrence 규칙을 포함합니다.")
    @GetMapping("/{scheduleId}")
    ResponseEntity<ApiResponse<ScheduleResponse>> getSchedule(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @PathVariable Long scheduleId
    );

    @Operation(
            summary = "개인 일정 부분 수정",
            description = "recurring은 null이면 유지, false이면 해제, true이면 recurrence 규칙을 적용합니다."
    )
    @PatchMapping("/{scheduleId}")
    ResponseEntity<ApiResponse<ScheduleResponse>> updateSchedule(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @PathVariable Long scheduleId,
            @Valid @RequestBody UpdateScheduleRequest request
    );

    @Operation(
            summary = "반복 일정 회차 수정",
            description = "occurrenceDate는 규칙이 만들어낸 원본 회차 날짜입니다. "
                    + "date를 보내면 그 회차만 다른 날로 옮깁니다."
    )
    @PatchMapping("/{scheduleId}/occurrences/{occurrenceDate}")
    ResponseEntity<ApiResponse<OccurrenceResponse>> updateOccurrence(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @PathVariable Long scheduleId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate occurrenceDate,
            @Valid @RequestBody UpdateOccurrenceRequest request
    );

    @Operation(summary = "반복 일정 회차 삭제", description = "해당 회차만 건너뜁니다.")
    @DeleteMapping("/{scheduleId}/occurrences/{occurrenceDate}")
    ResponseEntity<ApiResponse<Void>> deleteOccurrence(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @PathVariable Long scheduleId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate occurrenceDate
    );

    @Operation(summary = "개인 일정 삭제")
    @DeleteMapping("/{scheduleId}")
    ResponseEntity<ApiResponse<Void>> deleteSchedule(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @PathVariable Long scheduleId
    );
}
