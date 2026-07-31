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
import yooze.withme.domain.auth.repository.UserRepository;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserCommandService {

    private final UserRepository userRepository;

    /** 신규 사용자 생성 */
    public User registerUser(String nickname, String email) {
        User user = User.builder()
                .nickname(nickname)
                .email(email)
                .kakaoSync(false)
                .notifyAgree(false)
                .build();

        User savedUser = userRepository.save(user);
        return savedUser;
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
}
