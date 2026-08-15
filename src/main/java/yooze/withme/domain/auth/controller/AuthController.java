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
import yooze.withme.domain.auth.dto.request.KakaoCallbackRequest;
import yooze.withme.domain.auth.dto.request.LoginRequest;
import yooze.withme.domain.auth.dto.request.SignUpRequest;
import yooze.withme.domain.auth.dto.response.KakaoLoginResponse;
import yooze.withme.domain.auth.dto.response.LoginResponse;
import yooze.withme.domain.auth.dto.response.SignUpResponse;
import yooze.withme.domain.auth.service.AuthCommandService;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

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

    @PostMapping("/kakao/callback")
    public ResponseEntity<ApiResponse<KakaoLoginResponse>> postKakaoCallback(
            @Valid @RequestBody KakaoCallbackRequest request
    ) {
        KakaoLoginResponse kakaoLoginResponse = authCommandService.kakaoLogin(request.code());
        return ApiResponse.success(SuccessStatus.KAKAO_LOGIN_SUCCESS, kakaoLoginResponse);
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
}
