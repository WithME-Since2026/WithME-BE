package yooze.withme.domain.group.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.domain.group.dto.request.RescheduleGroupRoundRequest;
import yooze.withme.domain.group.dto.request.SubmitGroupResponseRequest;
import yooze.withme.domain.group.dto.response.AttendanceResponse;
import yooze.withme.domain.group.dto.response.GroupRoundResponse;

@Tag(name = "모임 회차", description = "모임 회차 일정 변경 및 참석 응답 API")
public interface GroupRoundControllerDocs {

    @Operation(summary = "모임 일정 변경", description = "OWNER/CO_OWNER만 가능하며, 변경 시 해당 회차의 모든 참석 응답이 재확인(RERESPONSE) 상태로 전환된다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "일정 변경 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "운영자가 아니어서 접근 불가")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 회차")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{roundId}/reschedule")
    ResponseEntity<ApiResponse<GroupRoundResponse>> rescheduleGroupRound(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long roundId,
            @Valid @RequestBody RescheduleGroupRoundRequest request
    );

    @Operation(summary = "특정 회차의 참석 현황 목록 조회", description = "운영자(OWNER/CO_OWNER)만 조회할 수 있다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "참석 현황 조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "운영자가 아니어서 접근 불가")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{roundId}/group-responses")
    ResponseEntity<ApiResponse<List<AttendanceResponse>>> getGroupResponses(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long roundId
    );

    @Operation(summary = "참석 응답 제출", description = "참여자 본인의 출석 여부(ATTEND/ABSENT)를 제출하거나 수정한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "참석 응답 제출 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "모임 멤버가 아니거나 응답 정보를 찾을 수 없음")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{roundId}/group-responses")
    ResponseEntity<ApiResponse<AttendanceResponse>> submitGroupResponse(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long roundId,
            @Valid @RequestBody SubmitGroupResponseRequest request
    );
}
