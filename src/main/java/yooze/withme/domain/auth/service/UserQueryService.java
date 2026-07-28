package yooze.withme.domain.auth.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.auth.dto.response.AttendanceRateResponse;
import yooze.withme.domain.auth.dto.response.MyGroupResponse;
import yooze.withme.domain.auth.dto.response.NotificationSettingsResponse;
import yooze.withme.domain.auth.dto.response.ProfileResponse;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.auth.repository.UserRepository;
import yooze.withme.domain.group.enums.AttendanceStatus;
import yooze.withme.domain.group.enums.GroupMemberStatus;
import yooze.withme.domain.group.repository.GroupMemberRepository;
import yooze.withme.domain.group.repository.GroupResponseRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserQueryService {

    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupResponseRepository groupResponseRepository;

    /** userId로 사용자 조회 */
    public User getUserByUserId(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
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

    /** 내 참여율 조회 - 결정된(ATTEND/ABSENT) 응답 중 ATTEND 비율 */
    public AttendanceRateResponse getUserAttendanceRate(Long userId) {
        long attendCount = groupResponseRepository.countByMember_UserIdAndAttendanceStatus(userId, AttendanceStatus.ATTEND);
        long absentCount = groupResponseRepository.countByMember_UserIdAndAttendanceStatus(userId, AttendanceStatus.ABSENT);
        return AttendanceRateResponse.of(attendCount, absentCount);
    }

    /** 내가 활동 중인 모임 목록 조회 */
    public List<MyGroupResponse> getUserGroups(Long userId) {
        return groupMemberRepository.findByUserIdAndStatus(userId, GroupMemberStatus.ACTIVE).stream()
                .map(MyGroupResponse::from)
                .toList();
    }
}
