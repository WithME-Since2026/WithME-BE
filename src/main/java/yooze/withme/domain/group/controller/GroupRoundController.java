package yooze.withme.domain.group.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.common.status.success.SuccessStatus;
import yooze.withme.domain.group.controller.docs.GroupRoundControllerDocs;
import yooze.withme.domain.group.dto.request.RescheduleGroupRoundRequest;
import yooze.withme.domain.group.dto.request.SubmitGroupResponseRequest;
import yooze.withme.domain.group.dto.response.AttendanceResponse;
import yooze.withme.domain.group.dto.response.GroupRoundResponse;
import yooze.withme.domain.group.service.GroupCommandService;
import yooze.withme.domain.group.service.GroupQueryService;

@RestController
@RequestMapping("/api/v1/group-rounds")
@RequiredArgsConstructor
public class GroupRoundController implements GroupRoundControllerDocs {

    private final GroupCommandService groupCommandService;
    private final GroupQueryService groupQueryService;

    @Override
    public ResponseEntity<ApiResponse<GroupRoundResponse>> rescheduleGroupRound(
            Long userId, Long roundId, RescheduleGroupRoundRequest request
    ) {
        GroupRoundResponse response = groupCommandService.rescheduleGroupRound(userId, roundId, request);
        return ApiResponse.success(SuccessStatus.RESCHEDULE_GROUP_ROUND_SUCCESS, response);
    }

    @Override
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getGroupResponses(Long userId, Long roundId) {
        List<AttendanceResponse> response = groupQueryService.getGroupResponses(userId, roundId);
        return ApiResponse.success(SuccessStatus.GET_GROUP_RESPONSES_SUCCESS, response);
    }

    @Override
    public ResponseEntity<ApiResponse<AttendanceResponse>> submitGroupResponse(
            Long userId, Long roundId, SubmitGroupResponseRequest request
    ) {
        AttendanceResponse response = groupCommandService.submitGroupResponse(userId, roundId, request);
        return ApiResponse.success(SuccessStatus.SUBMIT_GROUP_RESPONSE_SUCCESS, response);
    }
}
