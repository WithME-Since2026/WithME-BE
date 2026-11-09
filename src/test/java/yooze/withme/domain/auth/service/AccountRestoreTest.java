package yooze.withme.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.jwt.JwtTokenProvider;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.auth.dto.request.LoginRequest;
import yooze.withme.domain.auth.dto.response.LoginResponse;
import yooze.withme.domain.auth.dto.response.RestoreTokenResponse;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.auth.entity.UserAuth;
import yooze.withme.domain.auth.enums.ProviderType;
import yooze.withme.domain.auth.repository.RestoreTokenRedisRepository;
import yooze.withme.domain.auth.repository.RestoreTokenRedisRepository.RestoreTarget;
import yooze.withme.domain.auth.repository.UserAuthRepository;

@ExtendWith(MockitoExtension.class)
class AccountRestoreTest {

    private static final Long USER_ID = 1L;
    private static final Long KAKAO_ID = 100L;
    private static final String RESTORE_TOKEN = "restore-token";

    @Mock private UserAuthRepository userAuthRepository;
    @Mock private UserQueryService userQueryService;
    @Mock private UserTokenCommandService userTokenCommandService;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RestoreTokenRedisRepository restoreTokenRedisRepository;

    @InjectMocks private AuthCommandService authCommandService;
    @InjectMocks private UserCommandService userCommandService;

    @Test
    void localLoginOfDeletedUserReturnsRestoreTokenWithoutRestoring() {
        User user = deletedUser();
        UserAuth auth = UserAuth.builder().user(user).provider(ProviderType.LOCAL).localId("id").password("hash").build();
        when(userAuthRepository.findByLocalIdAndProvider("id", ProviderType.LOCAL)).thenReturn(Optional.of(auth));
        when(passwordEncoder.matches("pw", "hash")).thenReturn(true);
        when(restoreTokenRedisRepository.issue(USER_ID, ProviderType.LOCAL)).thenReturn(RESTORE_TOKEN);

        assertDeletedUserWithRestoreToken(() -> authCommandService.login(loginRequest()));

        assertThat(user.getDeletedAt()).isNotNull();
    }

    @Test
    void kakaoLoginOfDeletedUserReturnsRestoreTokenWithoutRestoring() {
        User user = deletedUser();
        UserAuth auth = UserAuth.builder().user(user).provider(ProviderType.KAKAO).providerUserId(KAKAO_ID).build();
        when(userAuthRepository.findByProviderAndProviderUserId(ProviderType.KAKAO, KAKAO_ID)).thenReturn(Optional.of(auth));
        when(restoreTokenRedisRepository.issue(USER_ID, ProviderType.KAKAO)).thenReturn(RESTORE_TOKEN);

        assertDeletedUserWithRestoreToken(
                () -> userCommandService.findOrRegisterKakaoUser(KAKAO_ID, "a@a.com", "nick"));

        assertThat(user.getDeletedAt()).isNotNull();
    }

    @Test
    void wrongPasswordOfDeletedUserGetsNoRestoreToken() {
        UserAuth auth = UserAuth.builder().user(deletedUser()).provider(ProviderType.LOCAL).localId("id").password("hash").build();
        when(userAuthRepository.findByLocalIdAndProvider("id", ProviderType.LOCAL)).thenReturn(Optional.of(auth));
        when(passwordEncoder.matches("pw", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authCommandService.login(loginRequest()))
                .isInstanceOfSatisfying(GeneralException.class, e -> {
                    assertThat(e.getErrorStatus()).isEqualTo(ErrorStatus.INVALID_CREDENTIALS);
                    assertThat(e.getData()).isNull();
                });
    }

    @Test
    void restoreRevivesUserAndIssuesTokensForOriginalProvider() {
        User user = deletedUser();
        when(restoreTokenRedisRepository.consume(RESTORE_TOKEN))
                .thenReturn(Optional.of(new RestoreTarget(USER_ID, ProviderType.KAKAO)));
        when(userQueryService.getUserByUserId(USER_ID)).thenReturn(user);
        when(jwtTokenProvider.generateAccessToken(USER_ID)).thenReturn("access");
        when(jwtTokenProvider.generateRefreshToken(USER_ID, ProviderType.KAKAO)).thenReturn("refresh");

        LoginResponse response = authCommandService.restore(RESTORE_TOKEN);

        assertThat(user.getDeletedAt()).isNull();
        assertThat(response).isEqualTo(LoginResponse.of("access", "refresh"));
        verify(userTokenCommandService).issueToken(any(), any(), any(), any());
    }

    @Test
    void restoreRejectsUnknownToken() {
        when(restoreTokenRedisRepository.consume(RESTORE_TOKEN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authCommandService.restore(RESTORE_TOKEN))
                .isInstanceOfSatisfying(GeneralException.class,
                        e -> assertThat(e.getErrorStatus()).isEqualTo(ErrorStatus.INVALID_RESTORE_TOKEN));
    }

    @Test
    @SuppressWarnings("unchecked")
    void restoreTokenCanBeConsumedOnlyOnce() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("auth:restore:" + RESTORE_TOKEN)).thenReturn("1:KAKAO", (String) null);
        RestoreTokenRedisRepository repository = new RestoreTokenRedisRepository(redisTemplate);

        assertThat(repository.consume(RESTORE_TOKEN)).contains(new RestoreTarget(USER_ID, ProviderType.KAKAO));
        assertThat(repository.consume(RESTORE_TOKEN)).isEmpty();
    }

    private void assertDeletedUserWithRestoreToken(ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOfSatisfying(GeneralException.class, e -> {
            assertThat(e.getErrorStatus()).isEqualTo(ErrorStatus.DELETED_USER);
            assertThat(e.getData()).isEqualTo(new RestoreTokenResponse(RESTORE_TOKEN));
        });
    }

    private User deletedUser() {
        return User.builder().userId(USER_ID).email("a@a.com").nickname("nick").deletedAt(LocalDateTime.now()).build();
    }

    private LoginRequest loginRequest() {
        return new LoginRequest("id", "pw");
    }
}
