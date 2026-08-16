package yooze.withme.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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

import java.util.Optional;

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
     * 동시 요청으로 INSERT가 충돌하면 DataIntegrityViolationException을 잡아 재조회한다.
     *
     * @return (user, newUser)
     */
    public KakaoUserResult findOrRegisterKakaoUser(Long kakaoId, String email, String nickname) {
        // 기존 카카오 연동 계정 확인
        Optional<UserAuth> existing = userAuthRepository.findByProviderAndProviderUserId(ProviderType.KAKAO, kakaoId);
        if (existing.isPresent()) {
            return new KakaoUserResult(existing.get().getUser(), false);
        }

        // 신규 등록 시도
        try {
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

        } catch (DataIntegrityViolationException e) {
            // 동시 요청이 먼저 INSERT한 경우 → 재조회해서 기존 유저로 로그인
            // 재조회에도 없으면 email이 다른 계정(로컬 등)에 이미 사용 중인 것
            return userAuthRepository.findByProviderAndProviderUserId(ProviderType.KAKAO, kakaoId)
                    .map(auth -> new KakaoUserResult(auth.getUser(), false))
                    .orElseThrow(() -> new GeneralException(ErrorStatus.EMAIL_ALREADY_REGISTERED));
        }
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
