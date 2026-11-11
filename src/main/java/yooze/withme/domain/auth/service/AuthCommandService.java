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
import yooze.withme.domain.auth.dto.response.RestoreTokenResponse;
import yooze.withme.domain.auth.dto.response.SignUpResponse;
import yooze.withme.domain.auth.dto.response.TokenReissueResponse;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.auth.entity.UserAuth;
import yooze.withme.domain.auth.enums.PlatformType;
import yooze.withme.domain.auth.enums.ProviderType;
import org.springframework.web.util.UriComponentsBuilder;
import yooze.withme.domain.auth.repository.OAuthStateRedisRepository;
import yooze.withme.domain.auth.repository.RestoreTokenRedisRepository;
import yooze.withme.domain.auth.repository.RestoreTokenRedisRepository.RestoreTarget;
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
    private final RestoreTokenRedisRepository restoreTokenRedisRepository;
    private final yooze.withme.domain.notification.service.NotificationCommandService notificationCommandService;

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

        User user = userCommandService.registerUser(signUpRequest.nickname(),signUpRequest.email());

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
        if (user.getDeletedAt() != null) {
            String restoreToken = restoreTokenRedisRepository.issue(user.getUserId(), ProviderType.LOCAL);
            throw new GeneralException(ErrorStatus.DELETED_USER, new RestoreTokenResponse(restoreToken));
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUserId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId(), ProviderType.LOCAL);
        LocalDateTime refreshExpiredAt = jwtTokenProvider.getRefreshTokenExpiredAt();

        userTokenCommandService.issueToken(user, refreshToken, ProviderType.LOCAL, refreshExpiredAt);

        log.info("로컬 로그인 성공 - userId: {}", user.getUserId());
        return LoginResponse.of(accessToken, refreshToken);
    }

    /** 탈퇴 계정 복구 — 로그인 때 받은 일회성 토큰으로 복구하고 바로 로그인시킨다 */
    public LoginResponse restore(String restoreToken) {
        RestoreTarget target = restoreTokenRedisRepository.consume(restoreToken)
                .orElseThrow(() -> new GeneralException(ErrorStatus.INVALID_RESTORE_TOKEN));

        // 프로필·설정만 복구한다. 모임 멤버십은 탈퇴 때 지웠으므로 재초대로 다시 참여한다
        User user = userQueryService.getUserByUserId(target.userId());
        user.restore();

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUserId());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId(), target.provider());
        LocalDateTime refreshExpiredAt = jwtTokenProvider.getRefreshTokenExpiredAt();

        userTokenCommandService.issueToken(user, refreshToken, target.provider(), refreshExpiredAt);

        log.info("탈퇴 계정 복구 - userId: {}, provider: {}", user.getUserId(), target.provider());
        return LoginResponse.of(accessToken, refreshToken);
    }

    /**
     * 카카오 OAuth 로그인 시작 시 state 생성.
     * platform 에 따라 앱/웹용 redirect_uri 를 선택해 카카오 로그인 URL 을 만들고,
     * state 와 redirect_uri 를 Redis 에 함께 저장한다.
     * 콜백에서 카카오 토큰 요청 시 같은 redirect_uri 를 써야 하므로 state 에 묶어 관리한다.
     * platform 미전달 시 APP 으로 처리해 기존 앱 호환을 유지한다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public KakaoStateResponse generateKakaoState(PlatformType platform) {
        String state = UUID.randomUUID().toString();
        String redirectUri = kakaoProperties.redirectUriFor(platform);
        oAuthStateRedisRepository.save(state, redirectUri);

        // 쿼리 파라미터를 직접 문자열로 이어붙이면 특수문자가 그대로 노출될 수 있으므로
        // UriComponentsBuilder 로 빌드해 안전하게 인코딩한다.
        String kakaoLoginUrl = UriComponentsBuilder
                .fromUriString("https://kauth.kakao.com/oauth/authorize")
                .queryParam("client_id", kakaoProperties.clientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("state", state)
                .build()
                .toUriString();

        return new KakaoStateResponse(state, kakaoLoginUrl);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public KakaoLoginResponse kakaoLogin(String authorizationCode, String state) {
        // 0. CSRF 방지 — state 일회성 검증 (존재하면 즉시 삭제) + 저장된 redirect_uri 반환
        // state 생성 시 플랫폼별 redirect_uri 를 함께 저장했으므로,
        // 여기서 꺼낸 URI 를 카카오 토큰 요청에 그대로 사용한다.
        // (카카오는 인가 요청과 토큰 요청의 redirect_uri 가 반드시 일치해야 한다)
        String redirectUri = oAuthStateRedisRepository.consumeState(state);
        if (redirectUri == null) {
            throw new GeneralException(ErrorStatus.INVALID_OAUTH_STATE);
        }

        // 1. 외부 HTTP 호출 — 트랜잭션(DB 커넥션) 없이 실행
        KakaoTokenResponse kakaoToken = kakaoAuthClient.getToken(authorizationCode, redirectUri);
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

    /** 로그아웃 — JWT 토큰 폐기 + FCM 토큰 삭제 (deviceId 제공 시) */
    public void logout(Long userId, String deviceId) {
        User user = userQueryService.getUserByUserId(userId);
        userAuthRepository.findAllByUser(user)
                .forEach(auth -> userTokenCommandService.revokeToken(user, auth.getProvider()));
        if (deviceId != null && !deviceId.isBlank()) {
            notificationCommandService.removeFcmToken(userId, deviceId);
            log.info("FCM 토큰 삭제 - userId: {}, deviceId: {}", userId, deviceId);
        }
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
