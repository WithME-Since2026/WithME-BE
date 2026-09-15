package yooze.withme.domain.auth.dto.request;

/**
 * 로그아웃 요청.
 * deviceId 는 선택값 — 제공 시 해당 기기의 FCM 토큰을 삭제한다.
 */
public record LogoutRequest(String deviceId) {}
