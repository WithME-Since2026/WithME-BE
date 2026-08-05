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
import yooze.withme.domain.group.controller.docs.GroupControllerDocs;
import yooze.withme.domain.group.dto.request.CreateGroupRequest;
import yooze.withme.domain.group.dto.response.GroupDetailResponse;
import yooze.withme.domain.group.dto.response.GroupRoundResponse;
import yooze.withme.domain.group.service.GroupCommandService;
import yooze.withme.domain.group.service.GroupQueryService;

// TODO: 이 클래스의 Long.parseLong(userDetails.getUsername()) 패턴은 TodoController에서 시작한
// userId 전달 방식(전역 통일 예정)으로 추후 함께 변경 필요.
@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController implements GroupControllerDocs {

    private final GroupCommandService groupCommandService;
    private final GroupQueryService groupQueryService;

    @Override
    public ResponseEntity<ApiResponse<GroupDetailResponse>> createGroup(
            @AuthenticationPrincipal UserDetails userDetails, CreateGroupRequest request
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        GroupDetailResponse response = groupCommandService.createGroup(userId, request);
        return ApiResponse.success(SuccessStatus.CREATE_GROUP_SUCCESS, response);
    }

    @Override
    public ResponseEntity<ApiResponse<GroupDetailResponse>> getGroupDetail(Long groupId) {
        GroupDetailResponse response = groupQueryService.getGroupDetail(groupId);
        return ApiResponse.success(SuccessStatus.GET_GROUP_SUCCESS, response);
    }

    @Override
    public ResponseEntity<ApiResponse<List<GroupRoundResponse>>> getGroupRounds(Long groupId) {
        List<GroupRoundResponse> response = groupQueryService.getGroupRounds(groupId);
        return ApiResponse.success(SuccessStatus.GET_GROUP_ROUNDS_SUCCESS, response);
    }
}
