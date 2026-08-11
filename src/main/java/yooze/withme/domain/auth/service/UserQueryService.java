package yooze.withme.domain.auth.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.auth.dto.response.NotificationSettingsResponse;
import yooze.withme.domain.auth.dto.response.ProfileResponse;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.auth.repository.UserRepository;
import yooze.withme.domain.group.dto.response.AttendanceRateResponse;
import yooze.withme.domain.group.dto.response.MyGroupResponse;
import yooze.withme.domain.group.service.GroupQueryService;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserQueryService {

    private final UserRepository userRepository;
    private final GroupQueryService groupQueryService;

    /** userId로 사용자 조회 */
    public User getUserByUserId(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
    }

    /** 닉네임 + 이메일로 사용자 조회 */
    public User getUserByNicknameAndEmail(String nickname, String email) {
        return userRepository.findByNicknameAndEmail(nickname, email)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND_BY_INFO));
    }

    /** 이메일로 사용자 조회 */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
    }

    /** 이메일 존재 여부 확인 */
    public boolean existsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    /** 내 프로필 조회 */
    public ProfileResponse getUserProfile(Long userId) {
        User user = getUserByUserId(userId);
        return ProfileResponse.from(user);
    }

    /** 내 알림 설정 조회 */
    public NotificationSettingsResponse getUserNotificationSettings(Long userId) {
        User user = getUserByUserId(userId);
        return NotificationSettingsResponse.from(user);
    }

    /** 내 참여율 조회 - 사용자 존재를 확인한 뒤 Group 도메인의 계산 결과를 그대로 전달 */
    public AttendanceRateResponse getUserAttendanceRate(Long userId) {
        getUserByUserId(userId);
        return groupQueryService.getUserAttendanceRate(userId);
    }

    /** 내가 활동 중인 모임 목록 조회 - 사용자 존재를 확인한 뒤 Group 도메인의 조회 결과를 그대로 전달 */
    public List<MyGroupResponse> getUserGroups(Long userId) {
        getUserByUserId(userId);
        return groupQueryService.getUserGroups(userId);
    }
}
