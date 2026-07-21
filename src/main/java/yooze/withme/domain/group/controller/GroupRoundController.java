package yooze.withme.domain.group.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.common.status.success.SuccessStatus;
import yooze.withme.domain.group.dto.request.RescheduleGroupRoundRequest;
import yooze.withme.domain.group.dto.request.SubmitGroupResponseRequest;
import yooze.withme.domain.group.dto.response.AttendanceResponse;
import yooze.withme.domain.group.dto.response.GroupRoundResponse;
import yooze.withme.domain.group.service.GroupCommandService;
import yooze.withme.domain.group.service.GroupQueryService;

@RestController
@RequestMapping("/api/v1/group-rounds")
@RequiredArgsConstructor
public class GroupRoundController {

    private final GroupCommandService groupCommandService;
    private final GroupQueryService groupQueryService;

    /** 모임 일정 변경 - OWNER/CO_OWNER만 가능, 변경 시 참여자 응답이 재확인 상태로 전환됨 */
    @PatchMapping("/{roundId}/reschedule")
    public ResponseEntity<ApiResponse<GroupRoundResponse>> rescheduleGroupRound(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long roundId,
            @Valid @RequestBody RescheduleGroupRoundRequest request
    ) {
        GroupRoundResponse response = groupCommandService.rescheduleGroupRound(userId, roundId, request);
        return ApiResponse.success(SuccessStatus.RESCHEDULE_GROUP_ROUND_SUCCESS, response);
    }

    /** 특정 회차의 참석 현황 목록 조회 (운영자 전용) */
    @GetMapping("/{roundId}/group-responses")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getGroupResponses(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long roundId
    ) {
        List<AttendanceResponse> response = groupQueryService.getGroupResponses(userId, roundId);
        return ApiResponse.success(SuccessStatus.GET_GROUP_RESPONSES_SUCCESS, response);
    }

    /** 참석 응답 제출 (참여자) */
    @PatchMapping("/{roundId}/group-responses")
    public ResponseEntity<ApiResponse<AttendanceResponse>> submitGroupResponse(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long roundId,
            @Valid @RequestBody SubmitGroupResponseRequest request
    ) {
        AttendanceResponse response = groupCommandService.submitGroupResponse(userId, roundId, request);
        return ApiResponse.success(SuccessStatus.SUBMIT_GROUP_RESPONSE_SUCCESS, response);
    }
}
