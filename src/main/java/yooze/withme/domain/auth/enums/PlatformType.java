package yooze.withme.domain.auth.enums;

/** 카카오 로그인을 시작한 플랫폼. redirect_uri 선택에 사용된다. */
public enum PlatformType {
    /** 모바일 앱 (기본값) */
    APP,
    /** 웹 브라우저 */
    WEB
}
