package yooze.withme.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.auth.dto.response.NotificationSettingsResponse;
import yooze.withme.domain.auth.dto.response.ProfileResponse;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.auth.entity.UserAuth;
import yooze.withme.domain.auth.enums.ProviderType;
import yooze.withme.domain.auth.repository.UserAuthRepository;
import yooze.withme.domain.auth.repository.UserRepository;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserCommandService {

    private final UserRepository userRepository;
    /**
     * 카카오 신규 사용자 등록(User + UserAuth)을 원자적으로 처리하기 위해 함께 보유.
     * 두 저장이 단일 트랜잭션 안에 있어야 하므로 별도 서비스 호출 대신 직접 참조한다.
     */
    private final UserAuthRepository userAuthRepository;

    /** 신규 사용자 생성 */
    public User registerUser(String nickname, String email) {
        User user = User.builder()
                .nickname(nickname)
                .email(email)
                .kakaoSync(false)
                .notifyAgree(false)
                .build();

        return userRepository.save(user);
    }

    /**
     * 카카오 로그인 — 기존 연동 사용자면 조회, 신규면 User + UserAuth 함께 등록.
     * 외부 HTTP 호출(카카오 API) 이후에 실행되므로 이 메서드 자체가 트랜잭션 경계.
     *
     * @return (user, isNewUser)
     */
    public KakaoUserResult findOrRegisterKakaoUser(Long kakaoId, String email, String nickname) {
        return userAuthRepository.findByProviderAndProviderUserId(ProviderType.KAKAO, kakaoId)
                .map(auth -> new KakaoUserResult(auth.getUser(), false))
                .orElseGet(() -> {
                    if (userRepository.findByEmail(email).isPresent()) {
                        throw new GeneralException(ErrorStatus.DUPLICATE_EMAIL);
                    }

                    User user = userRepository.save(User.builder()
                            .nickname(nickname)
                            .email(email)
                            .kakaoSync(true)
                            .notifyAgree(false)
                            .build());

                    userAuthRepository.save(UserAuth.builder()
                            .user(user)
                            .provider(ProviderType.KAKAO)
                            .providerUserId(kakaoId)
                            .build());

                    return new KakaoUserResult(user, true);
                });
    }

    /** 닉네임(이름) 변경 */
    public ProfileResponse updateNickname(Long userId, String nickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        user.updateNickname(nickname);
        return ProfileResponse.from(user);
    }

    /** 알림 수신 동의 여부 변경 */
    public NotificationSettingsResponse updateNotificationSettings(Long userId, boolean notifyAgree) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        user.updateNotifyAgree(notifyAgree);
        return NotificationSettingsResponse.from(user);
    }

    public record KakaoUserResult(User user, boolean newUser) {}
}
