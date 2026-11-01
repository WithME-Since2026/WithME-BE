package yooze.withme.domain.group.service;

public interface GroupWithdrawalService {

    /** 회원의 모임 탈퇴와 참석 기록 삭제를 처리한다. */
    void withdrawUser(Long userId);
}
