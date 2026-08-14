package yooze.withme.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.jwt.JwtTokenProvider;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.auth.client.KakaoAuthClient;
import yooze.withme.domain.auth.client.dto.KakaoTokenResponse;
import yooze.withme.domain.auth.client.dto.KakaoUserInfoResponse;
import yooze.withme.domain.auth.dto.request.LoginRequest;
import yooze.withme.domain.auth.dto.request.SignUpRequest;
import yooze.withme.domain.auth.dto.response.KakaoLoginResponse;
import yooze.withme.domain.auth.dto.response.LoginResponse;
import yooze.withme.domain.auth.dto.response.SignUpResponse;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.auth.entity.UserAuth;
import yooze.withme.domain.auth.enums.ProviderType;
import yooze.withme.domain.auth.repository.UserAuthRepository;
import yooze.withme.domain.auth.service.UserCommandService.KakaoUserResult;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthCommandService {

    private final UserAuthRepository userAuthRepository;
    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;
    private final UserTokenCommandService userTokenCommandService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final KakaoAuthClient kakaoAuthClient;

    /** 로컬 회원가입 */
    public SignUpResponse signUp(SignUpRequest signUpRequest) {
        if (userAuthRepository.existsByLocalIdAndProvider(signUpRequest.localId(), ProviderType.LOCAL)) {
            throw new GeneralException(ErrorStatus.DUPLICATE_ID);
        }

        if (userQueryService.existsByEmail(signUpRequest.email())) {
            throw new GeneralException(ErrorStatus.DUPLICATE_EMAIL);
        }

        if (!signUpRequest.password().equals(signUpRequest.passwordConfirm())) {
            throw new GeneralException(ErrorStatus.PASSWORD_MISMATCH);
        }

        User user = userCommandService.registerUser(signUpRequest.localId(), signUpRequest.email());

        UserAuth userAuth = UserAuth.builder()
                .user(user)
                .provider(ProviderType.LOCAL)
                .localId(signUpRequest.localId())
                .password(passwordEncoder.encode(signUpRequest.password()))
                .build();
        userAuthRepository.save(userAuth);

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUserId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId());
        LocalDateTime refreshExpiredAt = jwtTokenProvider.getRefreshTokenExpiredAt();

        userTokenCommandService.issueToken(user, refreshToken, ProviderType.LOCAL, refreshExpiredAt);

        log.info("로컬 회원가입 완료 - userId: {}, localId: {}", user.getUserId(), signUpRequest.localId());
        return SignUpResponse.of(user, accessToken, refreshToken);
    }

    /** 로컬 로그인 */
    public LoginResponse login(LoginRequest loginRequest) {
        UserAuth userAuth = userAuthRepository.findByLocalIdAndProvider(loginRequest.localId(), ProviderType.LOCAL)
                .orElseThrow(() -> new GeneralException(ErrorStatus.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(loginRequest.password(), userAuth.getPassword())) {
            throw new GeneralException(ErrorStatus.INVALID_CREDENTIALS);
        }

        User user = userAuth.getUser();

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUserId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId());
        LocalDateTime refreshExpiredAt = jwtTokenProvider.getRefreshTokenExpiredAt();

        userTokenCommandService.issueToken(user, refreshToken, ProviderType.LOCAL, refreshExpiredAt);

        log.info("로컬 로그인 성공 - userId: {}", user.getUserId());
        return LoginResponse.of(accessToken, refreshToken);
    }

    /**
     * 카카오 로그인.
     * 카카오 서버와의 HTTP 왕복(토큰 발급 + 사용자 정보 조회)이 끝난 뒤에
     * DB 커넥션을 획득해야 하므로 트랜잭션을 열지 않는다.
     * DB 작업은 userCommandService.findOrRegisterKakaoUser() 내부 트랜잭션에서 처리된다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public KakaoLoginResponse kakaoLogin(String authorizationCode) {
        // 1. 외부 HTTP 호출 — 트랜잭션(DB 커넥션) 없이 실행
        KakaoTokenResponse kakaoToken = kakaoAuthClient.getToken(authorizationCode);
        KakaoUserInfoResponse kakaoUserInfo = kakaoAuthClient.getUserInfo(kakaoToken.accessToken());

        Long kakaoId = kakaoUserInfo.id();
        String email = kakaoUserInfo.extractEmail();
        if (email == null) {
            throw new GeneralException(ErrorStatus.KAKAO_EMAIL_REQUIRED);
        }
        String nickname = kakaoUserInfo.extractNickname() != null
                ? kakaoUserInfo.extractNickname()
                : "카카오사용자" + kakaoId;

        // 2. DB 조회·저장 — userCommandService 내부 @Transactional 에서 커넥션 획득·반환
        KakaoUserResult result = userCommandService.findOrRegisterKakaoUser(kakaoId, email, nickname);

        if (result.isNewUser()) {
            log.info("카카오 회원가입 완료 - userId: {}, kakaoId: {}", result.user().getUserId(), kakaoId);
        } else {
            log.info("카카오 로그인 성공 - userId: {}, kakaoId: {}", result.user().getUserId(), kakaoId);
        }

        // 3. JWT 발급 + Redis 저장 — 트랜잭션 불필요
        String accessToken = jwtTokenProvider.generateAccessToken(result.user().getUserId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(result.user().getUserId());
        LocalDateTime refreshExpiredAt = jwtTokenProvider.getRefreshTokenExpiredAt();
        userTokenCommandService.issueToken(result.user(), refreshToken, ProviderType.KAKAO, refreshExpiredAt);

        return KakaoLoginResponse.of(result.user(), accessToken, refreshToken, result.isNewUser());
    }

    /** 로그아웃 — Redis에서 모든 provider의 리프레시 토큰 삭제 */
    public void logout(Long userId) {
        User user = userQueryService.getUserByUserId(userId);
        for (ProviderType provider : ProviderType.values()) {
            userTokenCommandService.revokeToken(user, provider);
        }
        log.info("로그아웃 - userId: {}", userId);
    }

    /** 아이디 중복 확인 (true = 중복) */
    @Transactional(readOnly = true)
    public boolean checkUsernameDuplicate(String localId) {
        return userAuthRepository.existsByLocalIdAndProvider(localId, ProviderType.LOCAL);
    }
}
