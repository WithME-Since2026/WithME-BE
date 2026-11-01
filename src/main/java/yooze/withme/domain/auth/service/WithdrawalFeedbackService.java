package yooze.withme.domain.auth.service;

import yooze.withme.domain.auth.dto.request.WithdrawUserRequest;

public interface WithdrawalFeedbackService {

    /** 탈퇴 사유와 의견을 저장한다. */
    void createFeedback(Long userId, WithdrawUserRequest request);
}
