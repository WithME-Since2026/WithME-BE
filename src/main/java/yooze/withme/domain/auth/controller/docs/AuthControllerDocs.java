package yooze.withme.domain.auth.controller.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import yooze.withme.common.response.ApiResponse;
import yooze.withme.domain.auth.dto.request.IdCheckRequest;
import yooze.withme.domain.auth.dto.request.KakaoCallbackRequest;
import yooze.withme.domain.auth.dto.request.LoginRequest;
import yooze.withme.domain.auth.dto.request.SignUpRequest;
import yooze.withme.domain.auth.dto.request.TokenReissueRequest;
import yooze.withme.domain.auth.dto.response.KakaoLoginResponse;
import yooze.withme.domain.auth.dto.response.KakaoStateResponse;
import yooze.withme.domain.auth.dto.response.LoginResponse;
import yooze.withme.domain.auth.dto.response.SignUpResponse;
import yooze.withme.domain.auth.dto.response.TokenReissueResponse;

@Tag(name = "Auth", description = "회원가입 / 로그인 / 아이디 중복 확인 / 로그아웃 / 카카오 로그인 API")
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

    @Operation(
            summary = "카카오 로그인 시작 (state 발급)",
            description = "CSRF 방지용 state를 생성하고 카카오 로그인 URL을 반환한다. "
                    + "프론트는 kakaoLoginUrl로 리다이렉트하고, 카카오가 돌려준 code·state를 "
                    + "/kakao/callback 으로 그대로 전달해야 한다. state는 10분간 유효하다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "state 및 로그인 URL 발급 성공")
    @GetMapping("/kakao/state")
    ResponseEntity<ApiResponse<KakaoStateResponse>> getKakaoState();

    @Operation(
            summary = "카카오 로그인 콜백",
            description = "카카오에서 전달받은 인가 코드(code)와 state로 로그인한다. "
                    + "state가 유효하지 않으면 400을 반환한다. "
                    + "기존에 연동된 계정이 없으면 자동으로 회원가입 후 로그인한다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "카카오 로그인 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 state 또는 이메일 제공 동의 필요")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "카카오 인가 코드가 유효하지 않음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 해당 이메일로 로컬 계정이 존재함 — 아이디 로그인 후 마이페이지에서 카카오 연동 필요")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "카카오 서버 오류")
    @PostMapping("/kakao/callback")
    ResponseEntity<ApiResponse<KakaoLoginResponse>> postKakaoCallback(
            @Valid @RequestBody KakaoCallbackRequest request
    );

    @Operation(summary = "토큰 재발급", description = "리프레시 토큰으로 액세스 토큰과 리프레시 토큰을 재발급한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 재발급 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 리프레시 토큰")
    @PostMapping("/reissue")
    ResponseEntity<ApiResponse<TokenReissueResponse>> postReissue(
            @Valid @RequestBody TokenReissueRequest request
    );

    @Operation(summary = "아이디 중복 확인", description = "사용하려는 아이디가 이미 존재하는지 확인한다. 중복이면 409, 사용 가능하면 200을 반환한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용 가능한 아이디")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 사용 중인 아이디")
    @GetMapping("/id-check")
    ResponseEntity<ApiResponse<Void>> getIdCheck(
            @Valid @ModelAttribute IdCheckRequest idCheckRequest
    );

    @Operation(summary = "로그아웃", description = "로그인한 사용자의 리프레시 토큰을 삭제한다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    ResponseEntity<ApiResponse<Void>> postLogout(
            @Parameter(hidden = true) UserDetails userDetails
    );
}
