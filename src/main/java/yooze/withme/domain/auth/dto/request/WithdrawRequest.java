package yooze.withme.domain.auth.dto.request;

/** 회원 탈퇴 요청. reason 은 선택값 */
public record WithdrawRequest(String reason) {}
