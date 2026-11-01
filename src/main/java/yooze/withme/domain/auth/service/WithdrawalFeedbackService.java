package yooze.withme.domain.auth.service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import yooze.withme.domain.auth.dto.request.WithdrawalFeedbackRequest;

public interface WithdrawalFeedbackService {

    /** 호출자의 회원탈퇴 트랜잭션에서 사유와 의견을 저장한다. */
    void createFeedback(@NotNull @Positive Long userId, @NotNull @Valid WithdrawalFeedbackRequest request);
}
