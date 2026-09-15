package yooze.withme.domain.auth.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.domain.auth.dto.request.ResetPasswordRequest;
import yooze.withme.domain.auth.dto.request.SendCodeRequest;
import yooze.withme.domain.auth.dto.request.SendPasswordCodeRequest;
import yooze.withme.domain.auth.dto.request.VerifyCodeRequest;
import yooze.withme.domain.auth.dto.response.FindIdResponse;

@Tag(name = "Find Account", description = "아이디 찾기 / 비밀번호 찾기 API")
public interface FindAccountControllerDocs {

    @Operation(summary = "아이디 찾기 - 인증코드 발송", description = "닉네임과 이메일로 사용자를 확인하고 6자리 인증코드를 이메일로 발송한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증코드 발송 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청값이 올바르지 않음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "닉네임 또는 이메일이 일치하는 사용자 없음")
    @PostMapping("/find-id/send-code")
    ResponseEntity<ApiResponse<Void>> postFindIdSendCode(
            @Valid @RequestBody SendCodeRequest request
    );

    @Operation(summary = "아이디 찾기 - 인증코드 확인", description = "인증코드 검증 후 아이디를 반환한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "아이디 찾기 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "인증코드가 올바르지 않거나 만료됨")
    @PostMapping("/find-id/verify")
    ResponseEntity<ApiResponse<FindIdResponse>> postFindIdVerify(
            @Valid @RequestBody VerifyCodeRequest request
    );

    @Operation(summary = "비밀번호 찾기 - 인증코드 발송", description = "아이디와 이메일로 사용자를 확인하고 6자리 인증코드를 이메일로 발송한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증코드 발송 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청값이 올바르지 않음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "아이디 또는 이메일이 일치하는 사용자 없음")
    @PostMapping("/find-password/send-code")
    ResponseEntity<ApiResponse<Void>> postFindPasswordSendCode(
            @Valid @RequestBody SendPasswordCodeRequest request
    );

    @Operation(summary = "비밀번호 찾기 - 비밀번호 재설정", description = "인증코드 검증 후 새 비밀번호로 변경한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "비밀번호 재설정 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "인증코드가 올바르지 않거나 만료됨")
    @PostMapping("/find-password/reset")
    ResponseEntity<ApiResponse<Void>> postFindPasswordReset(
            @Valid @RequestBody ResetPasswordRequest request
    );
}
