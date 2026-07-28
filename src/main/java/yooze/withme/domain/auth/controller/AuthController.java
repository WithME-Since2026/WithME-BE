package yooze.withme.domain.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.common.status.SuccessStatus;
import yooze.withme.domain.auth.controller.docs.AuthControllerDocs;
import yooze.withme.domain.auth.dto.request.IdCheckRequest;
import yooze.withme.domain.auth.dto.request.LoginRequest;
import yooze.withme.domain.auth.dto.request.SendCodeRequest;
import yooze.withme.domain.auth.dto.request.SignUpRequest;
import yooze.withme.domain.auth.dto.request.VerifyCodeRequest;
import yooze.withme.domain.auth.dto.response.FindIdResponse;
import yooze.withme.domain.auth.dto.response.LoginResponse;
import yooze.withme.domain.auth.dto.response.SignUpResponse;
import yooze.withme.domain.auth.service.AuthCommandService;
import yooze.withme.domain.auth.service.FindIdCommandService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

    private final AuthCommandService authCommandService;
    private final FindIdCommandService findIdCommandService;

    @PostMapping("/sign-up")
    public ResponseEntity<ApiResponse<SignUpResponse>> postSignUp(
            @Valid @RequestBody SignUpRequest signUpRequest
    ) {
        SignUpResponse signUpResponse = authCommandService.signUp(signUpRequest);
        return ApiResponse.success(SuccessStatus.CREATE_USER_SUCCESS, signUpResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> postLogin(
            @Valid @RequestBody LoginRequest loginRequest
    ) {
        LoginResponse loginResponse = authCommandService.login(loginRequest);
        return ApiResponse.success(SuccessStatus.LOGIN_SUCCESS, loginResponse);
    }

    @GetMapping("/id-check")
    public ResponseEntity<ApiResponse<Void>> getIdCheck(
            @Valid IdCheckRequest idCheckRequest
    ) {
        boolean isDuplicate = authCommandService.checkUsernameDuplicate(idCheckRequest.localId());
        if (isDuplicate) {
            return ApiResponse.error(ErrorStatus.DUPLICATE_ID);
        }
        return ApiResponse.success(SuccessStatus.SUCCESS_200);
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> postLogout(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        authCommandService.logout(userId);
        return ApiResponse.success(SuccessStatus.LOGOUT_SUCCESS);
    }

    @PostMapping("/find-id/send-code")
    public ResponseEntity<ApiResponse<Void>> postFindIdSendCode(
            @Valid @RequestBody SendCodeRequest request
    ) {
        findIdCommandService.sendVerificationCode(request.name(), request.email());
        return ApiResponse.success(SuccessStatus.SEND_CODE_SUCCESS);
    }

    @PostMapping("/find-id/verify")
    public ResponseEntity<ApiResponse<FindIdResponse>> postFindIdVerify(
            @Valid @RequestBody VerifyCodeRequest request
    ) {
        FindIdResponse response = findIdCommandService.verifyCodeAndFindId(request.email(), request.code());
        return ApiResponse.success(SuccessStatus.FIND_ID_SUCCESS, response);
    }
}
