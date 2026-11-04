package yooze.withme.domain.auth.dto.response;

/** 탈퇴 계정 로그인 시 DELETED_USER 에러의 data 로 내려주는 복구용 토큰 */
public record RestoreTokenResponse(
        String restoreToken
) {
}
