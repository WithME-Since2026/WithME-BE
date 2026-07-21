package yooze.withme.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.status.error.ErrorStatus;
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
    public User registerUser(String nickname) {
        User user = User.builder()
                .nickname(nickname)
                .kakaoSync(false)
                .notifyAgree(false)
                .build();

        User savedUser = userRepository.save(user);
        log.info("신규 사용자 생성 - userId: {}", savedUser.getUserId());
        return savedUser;
    }

    /** 닉네임(이름) 변경 */
    public ProfileResponse updateNickname(Long userId, String nickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        user.updateNickname(nickname);
        log.info("닉네임 변경 - userId: {}, nickname: {}", userId, nickname);
        return ProfileResponse.from(user);
    }
}
