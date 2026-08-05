package yooze.withme.domain.group.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.common.status.SuccessStatus;
import yooze.withme.domain.group.controller.docs.GroupRoundControllerDocs;
import yooze.withme.domain.group.dto.request.RescheduleGroupRoundRequest;
import yooze.withme.domain.group.dto.request.SubmitGroupResponseRequest;
import yooze.withme.domain.group.dto.response.AttendanceResponse;
import yooze.withme.domain.group.dto.response.GroupRoundResponse;
import yooze.withme.domain.group.service.GroupCommandService;
import yooze.withme.domain.group.service.GroupQueryService;

// TODO: 이 클래스의 Long.parseLong(userDetails.getUsername()) 패턴은 TodoController에서 시작한
// userId 전달 방식(전역 통일 예정)으로 추후 함께 변경 필요.
@RestController
@RequestMapping("/api/v1/group-rounds")
@RequiredArgsConstructor
public class GroupRoundController implements GroupRoundControllerDocs {

    private final GroupCommandService groupCommandService;
    private final GroupQueryService groupQueryService;

    @Override
    public ResponseEntity<ApiResponse<GroupRoundResponse>> rescheduleGroupRound(
            @AuthenticationPrincipal UserDetails userDetails, Long roundId, RescheduleGroupRoundRequest request
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        GroupRoundResponse response = groupCommandService.rescheduleGroupRound(userId, roundId, request);
        return ApiResponse.success(SuccessStatus.RESCHEDULE_GROUP_ROUND_SUCCESS, response);
    }

    @Override
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getGroupResponses(
            @AuthenticationPrincipal UserDetails userDetails, Long roundId
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        List<AttendanceResponse> response = groupQueryService.getGroupResponses(userId, roundId);
        return ApiResponse.success(SuccessStatus.GET_GROUP_RESPONSES_SUCCESS, response);
    }

    @Override
    public ResponseEntity<ApiResponse<AttendanceResponse>> submitGroupResponse(
            @AuthenticationPrincipal UserDetails userDetails, Long roundId, SubmitGroupResponseRequest request
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        AttendanceResponse response = groupCommandService.submitGroupResponse(userId, roundId, request);
        return ApiResponse.success(SuccessStatus.SUBMIT_GROUP_RESPONSE_SUCCESS, response);
    }
}
