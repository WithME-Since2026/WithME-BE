package yooze.withme.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.jwt.JwtTokenProvider;
import yooze.withme.common.status.error.ErrorStatus;
import yooze.withme.domain.auth.dto.request.LoginRequest;
import yooze.withme.domain.auth.dto.request.SignUpRequest;
import yooze.withme.domain.auth.dto.response.LoginResponse;
import yooze.withme.domain.auth.dto.response.SignUpResponse;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.auth.entity.UserAuth;
import yooze.withme.domain.auth.enums.ProviderType;
import yooze.withme.domain.auth.repository.UserAuthRepository;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthCommandService {

    private final UserAuthRepository userAuthRepository;
    private final UserCommandService userCommandService;
    private final UserTokenCommandService userTokenCommandService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    /** 로컬 회원가입  */
    public SignUpResponse signUp(SignUpRequest signUpRequest) {
        if (userAuthRepository.existsByLocalIdAndProvider(signUpRequest.localId(), ProviderType.LOCAL)) {
            throw new GeneralException(ErrorStatus.DUPLICATE_ID);
        }

        if (!signUpRequest.password().equals(signUpRequest.passwordConfirm())) {
            throw new GeneralException(ErrorStatus.PASSWORD_MISMATCH);
        }

        User user = userCommandService.registerUser(signUpRequest.localId());

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

    /** 아이디 중복 확인 (true = 중복) */
    @Transactional(readOnly = true)
    public boolean checkUsernameDuplicate(String localId) {
        return userAuthRepository.existsByLocalIdAndProvider(localId, ProviderType.LOCAL);
    }
}
