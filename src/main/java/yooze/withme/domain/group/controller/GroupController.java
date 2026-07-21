package yooze.withme.domain.group.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.common.status.success.SuccessStatus;
import yooze.withme.domain.group.dto.request.CreateGroupRequest;
import yooze.withme.domain.group.dto.response.GroupDetailResponse;
import yooze.withme.domain.group.dto.response.GroupRoundResponse;
import yooze.withme.domain.group.service.GroupCommandService;
import yooze.withme.domain.group.service.GroupQueryService;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupCommandService groupCommandService;
    private final GroupQueryService groupQueryService;

    /** 모임 생성 */
    @PostMapping
    public ResponseEntity<ApiResponse<GroupDetailResponse>> createGroup(
            @RequestHeader("X-USER-ID") Long userId,
            @Valid @RequestBody CreateGroupRequest request
    ) {
        GroupDetailResponse response = groupCommandService.createGroup(userId, request);
        return ApiResponse.success(SuccessStatus.CREATE_GROUP_SUCCESS, response);
    }

    /** 모임 상세 조회 */
    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse<GroupDetailResponse>> getGroupDetail(@PathVariable Long groupId) {
        GroupDetailResponse response = groupQueryService.getGroupDetail(groupId);
        return ApiResponse.success(SuccessStatus.GET_GROUP_SUCCESS, response);
    }

    /** 모임 내 회차 목록 조회 */
    @GetMapping("/{groupId}/group-rounds")
    public ResponseEntity<ApiResponse<List<GroupRoundResponse>>> getGroupRounds(@PathVariable Long groupId) {
        List<GroupRoundResponse> response = groupQueryService.getGroupRounds(groupId);
        return ApiResponse.success(SuccessStatus.GET_GROUP_ROUNDS_SUCCESS, response);
    }
}
