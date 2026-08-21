package yooze.withme.domain.calendar.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.common.status.SuccessStatus;
import yooze.withme.domain.calendar.controller.docs.AdminHolidayControllerDocs;
import yooze.withme.domain.calendar.service.HolidaySyncService;

@RestController
@RequestMapping("/api/v1/admin/holidays")
@RequiredArgsConstructor
public class AdminHolidayController implements AdminHolidayControllerDocs {

    private final HolidaySyncService holidaySyncService;

    @Override
    public ResponseEntity<ApiResponse<Boolean>> syncHolidays(@RequestParam int year) {
        return ApiResponse.success(
                SuccessStatus.SYNC_HOLIDAY_SUCCESS,
                holidaySyncService.sync(year)
        );
    }
}
