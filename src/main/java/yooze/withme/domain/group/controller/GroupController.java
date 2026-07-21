package yooze.withme.domain.group.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "모임", description = "모임 생성/조회 API")
@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupCommandService groupCommandService;
    private final GroupQueryService groupQueryService;

    @Operation(summary = "모임 생성", description = "모임과 기본 장소, 최초 회차를 생성하고 생성자를 운영자(OWNER)로 등록한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "모임 생성 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청값이 올바르지 않음")
    @PostMapping
    public ResponseEntity<ApiResponse<GroupDetailResponse>> createGroup(
            @RequestHeader("X-USER-ID") Long userId,
            @Valid @RequestBody CreateGroupRequest request
    ) {
        GroupDetailResponse response = groupCommandService.createGroup(userId, request);
        return ApiResponse.success(SuccessStatus.CREATE_GROUP_SUCCESS, response);
    }

    @Operation(summary = "모임 상세 조회", description = "모임 ID로 모임의 상세 정보를 조회한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "모임 상세 조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 모임")
    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse<GroupDetailResponse>> getGroupDetail(@PathVariable Long groupId) {
        GroupDetailResponse response = groupQueryService.getGroupDetail(groupId);
        return ApiResponse.success(SuccessStatus.GET_GROUP_SUCCESS, response);
    }

    @Operation(summary = "모임 내 회차 목록 조회", description = "모임에 속한 모든 회차를 날짜/시간 오름차순으로 조회한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회차 목록 조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 모임")
    @GetMapping("/{groupId}/group-rounds")
    public ResponseEntity<ApiResponse<List<GroupRoundResponse>>> getGroupRounds(@PathVariable Long groupId) {
        List<GroupRoundResponse> response = groupQueryService.getGroupRounds(groupId);
        return ApiResponse.success(SuccessStatus.GET_GROUP_ROUNDS_SUCCESS, response);
    }
}
