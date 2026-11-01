package yooze.withme.domain.auth.dto.request;

import jakarta.validation.constraints.Size;

/** #42 회원탈퇴 요청에서 전달받는 선택 사유와 추가 의견. */
public record WithdrawalFeedbackRequest(
        @Size(max = 100) String reason,
        @Size(max = 1000) String opinion
) {}
