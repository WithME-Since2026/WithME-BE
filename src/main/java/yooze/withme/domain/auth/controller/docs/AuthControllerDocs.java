package yooze.withme.domain.auth.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.domain.auth.dto.request.IdCheckRequest;
import yooze.withme.domain.auth.dto.request.LoginRequest;
import yooze.withme.domain.auth.dto.request.SignUpRequest;
import yooze.withme.domain.auth.dto.response.LoginResponse;
import yooze.withme.domain.auth.dto.response.SignUpResponse;

@Tag(name = "인증", description = "회원가입 / 로그인 / 아이디 중복 확인 API")
public interface AuthControllerDocs {

    @Operation(summary = "회원가입", description = "아이디, 비밀번호으로 신규 회원을 등록한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "회원가입 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청값이 올바르지 않음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 사용 중인 아이디 또는 닉네임")
    @PostMapping("/sign-up")
    ResponseEntity<ApiResponse<SignUpResponse>> postSignUp(
            @Valid @RequestBody SignUpRequest signUpRequest
    );

    @Operation(summary = "로그인", description = "아이디와 비밀번호로 로그인하고 JWT 토큰을 발급받는다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "아이디 또는 비밀번호 불일치")
    @PostMapping("/login")
    ResponseEntity<ApiResponse<LoginResponse>> postLogin(
            @Valid @RequestBody LoginRequest loginRequest
    );

    @Operation(summary = "아이디 중복 확인", description = "사용하려는 아이디가 이미 존재하는지 확인한다. 중복이면 409, 사용 가능하면 200을 반환한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용 가능한 아이디")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 사용 중인 아이디")
    @GetMapping("/id-check")
    ResponseEntity<ApiResponse<Void>> getIdCheck(
            @Valid @ModelAttribute IdCheckRequest idCheckRequest
    );
}
