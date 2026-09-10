package yooze.withme.domain.group.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.domain.group.dto.request.CreateGroupRequest;
import yooze.withme.domain.group.dto.response.GroupDetailResponse;
import yooze.withme.domain.group.dto.response.GroupRoundResponse;

@Tag(name = "Group", description = "모임 생성/조회 API")
public interface GroupControllerDocs {

    @Operation(summary = "모임 생성", description = "모임과 기본 장소, 최초 회차를 생성하고 생성자를 운영자(OWNER)로 등록한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "모임 생성 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청값이 올바르지 않음")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    ResponseEntity<ApiResponse<GroupDetailResponse>> createGroup(
            @Parameter(hidden = true) UserDetails userDetails,
            @Valid @RequestBody CreateGroupRequest request
    );

    @Operation(summary = "모임 상세 조회", description = "모임 ID로 모임의 상세 정보를 조회한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "모임 상세 조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 모임")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{groupId}")
    ResponseEntity<ApiResponse<GroupDetailResponse>> getGroupDetail(@PathVariable Long groupId);

    @Operation(summary = "모임 내 회차 목록 조회", description = "모임에 속한 모든 회차를 날짜/시간 오름차순으로 조회한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회차 목록 조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 모임")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{groupId}/group-rounds")
    ResponseEntity<ApiResponse<List<GroupRoundResponse>>> getGroupRounds(@PathVariable Long groupId);
}
