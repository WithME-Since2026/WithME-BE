package yooze.withme.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 회원탈퇴 사유와 추가 의견. 화면의 선택 문구를 그대로 사유로 전달한다. */
public record WithdrawUserRequest(
        @NotBlank @Size(max = 100) String reason,
        @Size(max = 1000) String opinion
) {}
