package yooze.withme.domain.auth.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.domain.auth.dto.request.UpdateNicknameRequest;
import yooze.withme.domain.auth.dto.response.ProfileResponse;

@Tag(name = "유저", description = "유저 이름 입력 API")
public interface UserControllerDocs {

    @Operation(summary = "이름 입력", description = "회원가입한 사용자의 닉네임을 입력한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이름 입력 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청값이 올바르지 않음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    @PatchMapping("/me/profile")
    ResponseEntity<ApiResponse<ProfileResponse>> patchUserProfile(
            UserDetails userDetails,
            @Valid @RequestBody UpdateNicknameRequest request
    );
}
