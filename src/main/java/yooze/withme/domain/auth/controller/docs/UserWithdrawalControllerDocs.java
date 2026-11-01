package yooze.withme.domain.auth.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.domain.auth.dto.request.WithdrawUserRequest;

@Tag(name = "User", description = "회원탈퇴 API")
public interface UserWithdrawalControllerDocs {

    @Operation(summary = "회원탈퇴", description = "계정을 비활성화하고 모임에서 탈퇴하며 본인 참석 기록을 삭제한다. 사유와 의견을 저장하고 모든 로그인·푸시 토큰을 삭제한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "회원탈퇴 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "사유 누락 또는 입력 길이 초과")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "남은 멤버가 있는 모임의 모임장은 먼저 위임 필요")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/me")
    ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(hidden = true) UserDetails userDetails,
            @Valid @RequestBody WithdrawUserRequest request
    );
}
