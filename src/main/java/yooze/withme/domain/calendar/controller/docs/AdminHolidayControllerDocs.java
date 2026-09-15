package yooze.withme.domain.calendar.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import yooze.withme.common.response.ApiResponse;

@Tag(name = "Admin - Holiday", description = "공휴일 동기화 관리자 API (ROLE_ADMIN 전용)")
@SecurityRequirement(name = "bearerAuth")
public interface AdminHolidayControllerDocs {

    @Operation(
            summary = "공휴일 수동 동기화",
            description = "초기 적재/장애 복구용. 다른 인스턴스가 같은 해를 처리 중이면 실행하지 않고 false 를 돌려줍니다."
    )
    @PostMapping("/sync")
    ResponseEntity<ApiResponse<Boolean>> syncHolidays(@RequestParam int year);
}
