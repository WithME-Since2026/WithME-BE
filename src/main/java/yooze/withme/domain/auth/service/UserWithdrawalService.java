package yooze.withme.domain.auth.service;

import yooze.withme.domain.auth.dto.request.WithdrawUserRequest;

public interface UserWithdrawalService {

    /** 계정 비활성화, 모임 탈퇴, 참석 기록 삭제와 의견 저장을 함께 처리한다. */
    void withdrawUser(Long userId, WithdrawUserRequest request);
}
