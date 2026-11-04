package yooze.withme.domain.auth.dto.request;

import jakarta.validation.constraints.Size;

/** 회원탈퇴 시 전달받는 선택 사유와 추가 의견. */
public record WithdrawalFeedbackRequest(
        @Size(max = 100, message = "탈퇴 사유는 100자 이하여야 합니다.") String reason,
        @Size(max = 1000, message = "추가 의견은 1,000자 이하여야 합니다.") String opinion
) {}
