package yooze.withme.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
                .notifyGroupRemind(false)
                .notifyTodoDeadline(false)
                .notifyGroupInvite(false)
                .build();

        return userRepository.save(user);
    }

    /**
     * 카카오 로그인 — 기존 연동 사용자면 조회, 신규면 User + UserAuth 함께 등록.
     * 동시 요청으로 unique 충돌 시 DataIntegrityViolationException을 throw하며 트랜잭션이 롤백된다.
     * 호출자(AuthCommandService)가 catch하여 새 트랜잭션으로 findKakaoUser()를 호출한다.
     *
     * @return (user, newUser)
     */
    public KakaoUserResult findOrRegisterKakaoUser(Long kakaoId, String email, String nickname) {
        // 기존 카카오 연동 계정 확인
        Optional<UserAuth> existing = userAuthRepository.findByProviderAndProviderUserId(ProviderType.KAKAO, kakaoId);
        if (existing.isPresent()) {
            User user = existing.get().getUser();
            if (user.getDeletedAt() != null) {
                // TODO: 탈퇴 회원 재로그인/재가입 정책 논의 필요
                throw new GeneralException(ErrorStatus.DELETED_USER);
            }
            return new KakaoUserResult(user, false);
        }

        // 신규 등록 시도 — 동시 요청으로 unique 충돌 시 DataIntegrityViolationException을 그대로 throw.
        // 이 트랜잭션은 롤백되고, 호출자(AuthCommandService)가 새 트랜잭션으로 재조회한다.
        User user = userRepository.save(User.builder()
                .nickname(nickname)
                .email(email)
                .kakaoSync(true)
                .notifyGroupRemind(false)
                .notifyTodoDeadline(false)
                .notifyGroupInvite(false)
                .build());

        userAuthRepository.save(UserAuth.builder()
                .user(user)
                .provider(ProviderType.KAKAO)
                .providerUserId(kakaoId)
                .build());

        return new KakaoUserResult(user, true);
    }

    /**
     * 카카오 계정 재조회 — 동시 INSERT 충돌 후 롤백된 트랜잭션과 독립된 새 트랜잭션에서 실행된다.
     * 조회 결과가 없으면 다른 계정에 이미 사용 중인 이메일이므로 예외를 던진다.
     */
    @Transactional
    public KakaoUserResult findKakaoUser(Long kakaoId) {
        return userAuthRepository.findByProviderAndProviderUserId(ProviderType.KAKAO, kakaoId)
                .filter(auth -> auth.getUser().getDeletedAt() == null)
                .map(auth -> new KakaoUserResult(auth.getUser(), false))
                .orElseThrow(() -> new GeneralException(ErrorStatus.EMAIL_ALREADY_REGISTERED));
    }

    /** 닉네임(이름) 변경 */
    public ProfileResponse updateNickname(Long userId, String nickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        user.updateNickname(nickname);
        return ProfileResponse.from(user);
    }

    /** 알림 설정 변경 */
    public NotificationSettingsResponse updateNotificationSettings(
            Long userId, boolean notifyGroupRemind, boolean notifyTodoDeadline, boolean notifyGroupInvite) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        user.updateNotificationSettings(notifyGroupRemind, notifyTodoDeadline, notifyGroupInvite);
        return NotificationSettingsResponse.from(user);
    }

    public record KakaoUserResult(User user, boolean newUser) {}
}
