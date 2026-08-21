package yooze.withme.domain.calendar.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.domain.calendar.dto.response.CalendarItemResponse;

@Tag(name = "Calendar", description = "통합 캘린더 조회 API")
@SecurityRequirement(name = "bearerAuth")
public interface CalendarControllerDocs {

    @Operation(
            summary = "캘린더 조회",
            description = "기간 안의 내 todo·개인 일정·모임 회차·공휴일을 한 배열로 돌려줍니다. "
                    + "날짜 오름차순이고 같은 날에서는 종일 항목이 먼저 옵니다. "
                    + "반복 항목은 회차 단위로 펼쳐져 있으며, 개별 회차를 수정할 때는 "
                    + "sourceId와 occurrenceDate를 그대로 돌려보내면 됩니다. 최대 조회 기간은 92일입니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "캘린더 조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "from이 to보다 늦거나 조회 기간이 92일을 넘음"
    )
    @GetMapping
    ResponseEntity<ApiResponse<List<CalendarItemResponse>>> getCalendar(
            @Parameter(hidden = true) @RequestAttribute("userId") Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    );
}
