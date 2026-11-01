package yooze.withme.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.auth.dto.request.WithdrawUserRequest;
import yooze.withme.domain.auth.entity.User;
import yooze.withme.domain.auth.enums.ProviderType;
import yooze.withme.domain.auth.repository.UserRepository;
import yooze.withme.domain.group.service.GroupWithdrawalService;
import yooze.withme.domain.notification.service.NotificationCommandService;

@Service
@Transactional
@RequiredArgsConstructor
public class UserWithdrawalServiceImpl implements UserWithdrawalService {

    private final UserRepository userRepository;
    private final GroupWithdrawalService groupWithdrawalService;
    private final WithdrawalFeedbackService withdrawalFeedbackService;
    private final NotificationCommandService notificationCommandService;
    private final UserTokenCommandService userTokenCommandService;

    /** 사용자 잠금으로 중복 탈퇴를 막고 모든 DB 변경을 하나의 트랜잭션으로 처리한다. */
    @Override
    public void withdrawUser(Long userId, WithdrawUserRequest request) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));
        if (user.getDeletedAt() != null) {
            throw new GeneralException(ErrorStatus.DELETED_USER);
        }
        groupWithdrawalService.withdrawUser(userId);
        withdrawalFeedbackService.createFeedback(userId, request);
        notificationCommandService.removeAllFcmTokens(user);
        user.updateNotificationSettings(false, false, false);
        user.delete();
        // Redis 삭제 실패 시 DB는 롤백된다. 일부 토큰이 먼저 삭제되더라도 재로그인할 수 있다.
        for (ProviderType provider : ProviderType.values()) {
            userTokenCommandService.revokeToken(user, provider);
        }
    }
}
