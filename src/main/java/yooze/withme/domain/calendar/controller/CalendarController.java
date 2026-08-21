package yooze.withme.domain.calendar.controller;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.common.status.SuccessStatus;
import yooze.withme.domain.calendar.controller.docs.CalendarControllerDocs;
import yooze.withme.domain.calendar.dto.response.CalendarItemResponse;
import yooze.withme.domain.calendar.service.CalendarQueryService;

@RestController
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
public class CalendarController implements CalendarControllerDocs {

    private final CalendarQueryService calendarQueryService;

    @Override
    public ResponseEntity<ApiResponse<List<CalendarItemResponse>>> getCalendar(
            @RequestAttribute("userId") Long userId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to
    ) {
        List<CalendarItemResponse> response = calendarQueryService.getCalendar(userId, from, to);
        return ApiResponse.success(SuccessStatus.GET_CALENDAR_SUCCESS, response);
    }
}
