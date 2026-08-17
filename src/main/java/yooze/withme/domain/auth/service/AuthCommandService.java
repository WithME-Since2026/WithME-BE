package yooze.withme.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.jwt.JwtTokenProvider;
import yooze.withme.common.properties.KakaoProperties;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.auth.client.KakaoAuthClient;
import yooze.withme.domain.auth.client.dto.KakaoTokenResponse;
import yooze.withme.domain.auth.client.dto.KakaoUserInfoResponse;
import yooze.withme.domain.auth.dto.request.LoginRequest;
import yooze.withme.domain.auth.dto.request.SignUpRequest;
import yooze.withme.domain.auth.dto.response.KakaoLoginResponse;
import yooze.withme.domain.auth.dto.response.KakaoStateResponse;
import yooze.withme.domain.auth.dto.response.LoginResponse;
import yooze.withme.domain.auth.dto.response.SignUpResponse;
import yooze.withme.domain.auth.dto.response.TokenReissueResponse;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.auth.entity.UserAuth;
import yooze.withme.domain.auth.enums.ProviderType;
import yooze.withme.domain.auth.repository.OAuthStateRedisRepository;
import yooze.withme.domain.auth.repository.UserAuthRepository;
import yooze.withme.domain.auth.service.UserCommandService.KakaoUserResult;

import java.time.LocalDateTime;
import java.util.UUID;

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
    private final KakaoProperties kakaoProperties;
    private final OAuthStateRedisRepository oAuthStateRedisRepository;

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
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId(), ProviderType.LOCAL);
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
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId(), ProviderType.LOCAL);
        LocalDateTime refreshExpiredAt = jwtTokenProvider.getRefreshTokenExpiredAt();

        userTokenCommandService.issueToken(user, refreshToken, ProviderType.LOCAL, refreshExpiredAt);

        log.info("로컬 로그인 성공 - userId: {}", user.getUserId());
        return LoginResponse.of(accessToken, refreshToken);
    }

    /**
     * 카카오 OAuth 로그인 시작 시 state 생성.
     * 프론트는 반환된 kakaoLoginUrl 로 리다이렉트하고 카카오가 돌려준
     * code·state 를 /kakao/callback 으로 그대로 전달한다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public KakaoStateResponse generateKakaoState() {
        String state = UUID.randomUUID().toString();
        oAuthStateRedisRepository.save(state);

        String kakaoLoginUrl = "https://kauth.kakao.com/oauth/authorize"
                + "?client_id=" + kakaoProperties.clientId()
                + "&redirect_uri=" + kakaoProperties.redirectUri()
                + "&response_type=code"
                + "&state=" + state;

        return new KakaoStateResponse(state, kakaoLoginUrl);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public KakaoLoginResponse kakaoLogin(String authorizationCode, String state) {
        // 0. CSRF 방지 — state 일회성 검증 (존재하면 즉시 삭제)
        if (!oAuthStateRedisRepository.consumeState(state)) {
            throw new GeneralException(ErrorStatus.INVALID_OAUTH_STATE);
        }

        // 1. 외부 HTTP 호출 — 트랜잭션(DB 커넥션) 없이 실행
        KakaoTokenResponse kakaoToken = kakaoAuthClient.getToken(authorizationCode);
        KakaoUserInfoResponse kakaoUserInfo = kakaoAuthClient.getUserInfo(kakaoToken.accessToken());

        Long kakaoId = kakaoUserInfo.id();
        String email = kakaoUserInfo.extractEmail();
        if (email == null || !kakaoUserInfo.isEmailVerified()) {
            throw new GeneralException(ErrorStatus.KAKAO_EMAIL_REQUIRED);
        }
        String nickname = kakaoUserInfo.extractNickname() != null
                ? kakaoUserInfo.extractNickname()
                : "카카오사용자" + kakaoId;

        // 2. DB 조회·저장 — userCommandService 내부 @Transactional 에서 커넥션 획득·반환
        // 동시 요청으로 unique 충돌 시: 해당 트랜잭션이 롤백된 후 새 트랜잭션으로 재조회
        KakaoUserResult result;
        try {
            result = userCommandService.findOrRegisterKakaoUser(kakaoId, email, nickname);
        } catch (DataIntegrityViolationException e) {
            result = userCommandService.findKakaoUser(kakaoId);
        }

        if (result.newUser()) {
            log.info("카카오 회원가입 완료 - userId: {}, kakaoId: {}", result.user().getUserId(), kakaoId);
        } else {
            log.info("카카오 로그인 성공 - userId: {}, kakaoId: {}", result.user().getUserId(), kakaoId);
        }

        // 3. JWT 발급 + Redis 저장 — 트랜잭션 불필요
        String accessToken = jwtTokenProvider.generateAccessToken(result.user().getUserId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(result.user().getUserId(), ProviderType.KAKAO);
        LocalDateTime refreshExpiredAt = jwtTokenProvider.getRefreshTokenExpiredAt();
        userTokenCommandService.issueToken(result.user(), refreshToken, ProviderType.KAKAO, refreshExpiredAt);

        return KakaoLoginResponse.of(result.user(), accessToken, refreshToken, result.newUser());
    }

    /** 로그아웃 — 해당 유저의 실제 provider만 Redis에서 삭제 */
    public void logout(Long userId) {
        User user = userQueryService.getUserByUserId(userId);
        userAuthRepository.findAllByUser(user)
                .forEach(auth -> userTokenCommandService.revokeToken(user, auth.getProvider()));
        log.info("로그아웃 - userId: {}", userId);
    }

    /** 리프레시 토큰으로 액세스 토큰 재발급 (토큰 로테이션) */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public TokenReissueResponse reissueToken(String refreshToken) {
        jwtTokenProvider.validateToken(refreshToken);
        if (jwtTokenProvider.isAccessToken(refreshToken)) {
            throw new GeneralException(ErrorStatus.INVALID_TOKEN);
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        ProviderType provider = jwtTokenProvider.getProviderFromToken(refreshToken);

        if (!userTokenCommandService.matchesToken(userId, provider, refreshToken)) {
            throw new GeneralException(ErrorStatus.INVALID_TOKEN);
        }

        User user = userQueryService.getUserByUserId(userId);

        String newAccessToken = jwtTokenProvider.generateAccessToken(userId);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userId, provider);
        LocalDateTime refreshExpiredAt = jwtTokenProvider.getRefreshTokenExpiredAt();
        userTokenCommandService.issueToken(user, newRefreshToken, provider, refreshExpiredAt);

        log.info("토큰 재발급 - userId: {}, provider: {}", userId, provider);
        return new TokenReissueResponse(newAccessToken, newRefreshToken);
    }

    /** 아이디 중복 확인 (true = 중복) */
    @Transactional(readOnly = true)
    public boolean checkUsernameDuplicate(String localId) {
        return userAuthRepository.existsByLocalIdAndProvider(localId, ProviderType.LOCAL);
    }
}
