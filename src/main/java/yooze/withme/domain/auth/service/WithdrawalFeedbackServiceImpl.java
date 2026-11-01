package yooze.withme.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import yooze.withme.domain.auth.dto.request.WithdrawUserRequest;
import yooze.withme.domain.auth.entity.UserWithdrawal;
import yooze.withme.domain.auth.repository.UserWithdrawalRepository;

@Service
@Transactional
@RequiredArgsConstructor
public class WithdrawalFeedbackServiceImpl implements WithdrawalFeedbackService {

    private final UserWithdrawalRepository userWithdrawalRepository;

    /** 탈퇴 사유와 의견을 저장한다. 원문은 로그에 남기지 않는다. */
    @Override
    public void createFeedback(Long userId, WithdrawUserRequest request) {
        userWithdrawalRepository.save(UserWithdrawal.builder()
                .userId(userId)
                .reason(request.reason())
                .opinion(request.opinion())
                .build());
    }
}
