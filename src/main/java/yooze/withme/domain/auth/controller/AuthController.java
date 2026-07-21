package yooze.withme.domain.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.common.status.error.ErrorStatus;
import yooze.withme.common.status.success.SuccessStatus;
import yooze.withme.domain.auth.dto.request.IdCheckRequest;
import yooze.withme.domain.auth.dto.request.LoginRequest;
import yooze.withme.domain.auth.dto.request.SignUpRequest;
import yooze.withme.domain.auth.dto.response.LoginResponse;
import yooze.withme.domain.auth.dto.response.SignUpResponse;
import yooze.withme.domain.auth.service.AuthCommandService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthCommandService authCommandService;

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
}
