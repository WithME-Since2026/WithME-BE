package yooze.withme.domain.auth.enums;

/**
 * 사용자 권한. 승격 API는 두지 않고 운영 DB에서 직접 UPDATE 함.
 * (관리자 임명 API 자체가 권한 상승 공격면이라 필요한 관리자 수에 비해 값이 없음)
 */
public enum UserRole {
    USER,
    ADMIN
}
