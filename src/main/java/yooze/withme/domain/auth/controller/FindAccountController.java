package yooze.withme.domain.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.common.status.SuccessStatus;
import yooze.withme.domain.auth.controller.docs.FindAccountControllerDocs;
import yooze.withme.domain.auth.dto.request.ResetPasswordRequest;
import yooze.withme.domain.auth.dto.request.SendCodeRequest;
import yooze.withme.domain.auth.dto.request.SendPasswordCodeRequest;
import yooze.withme.domain.auth.dto.request.VerifyCodeRequest;
import yooze.withme.domain.auth.dto.response.FindIdResponse;
import yooze.withme.domain.auth.service.FindAccountCommandService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class FindAccountController implements FindAccountControllerDocs {

    private final FindAccountCommandService findAccountCommandService;

    @PostMapping("/find-id/send-code")
    public ResponseEntity<ApiResponse<Void>> postFindIdSendCode(
            @Valid @RequestBody SendCodeRequest request
    ) {
        findAccountCommandService.sendFindIdCode(request.nickname(), request.email());
        return ApiResponse.success(SuccessStatus.SEND_CODE_SUCCESS);
    }

    @PostMapping("/find-id/verify")
    public ResponseEntity<ApiResponse<FindIdResponse>> postFindIdVerify(
            @Valid @RequestBody VerifyCodeRequest request
    ) {
        FindIdResponse response = findAccountCommandService.verifyAndFindId(request.email(), request.code());
        return ApiResponse.success(SuccessStatus.FIND_ID_SUCCESS, response);
    }

    @PostMapping("/find-password/send-code")
    public ResponseEntity<ApiResponse<Void>> postFindPasswordSendCode(
            @Valid @RequestBody SendPasswordCodeRequest request
    ) {
        findAccountCommandService.sendFindPasswordCode(request.localId(), request.email());
        return ApiResponse.success(SuccessStatus.SEND_CODE_SUCCESS);
    }

    @PostMapping("/find-password/reset")
    public ResponseEntity<ApiResponse<Void>> postFindPasswordReset(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        findAccountCommandService.resetPassword(
                request.email(), request.code(), request.newPassword(), request.newPasswordConfirm()
        );
        return ApiResponse.success(SuccessStatus.RESET_PASSWORD_SUCCESS);
    }
}
