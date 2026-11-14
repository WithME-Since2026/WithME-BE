package yooze.withme.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import yooze.withme.common.exception.GeneralException;
import yooze.withme.common.status.ErrorStatus;
import yooze.withme.domain.auth.enums.PlatformType;

@ConfigurationProperties(prefix = "kakao")
public record KakaoProperties(
        String clientId,
        String clientSecret,
        /** 앱용 redirect_uri. 기존 KAKAO_REDIRECT_URI Secret과 호환 */
        String redirectUri,
        /** 웹용 redirect_uri. KAKAO_WEB_REDIRECT_URI Secret으로 주입 */
        String webRedirectUri,
        /** 회원 탈퇴 시 연결 끊기(unlink)에 쓰는 Admin 키. 비어 있으면 unlink 를 건너뛴다 */
        String adminKey
) {
    /**
     * 플랫폼에 맞는 redirect_uri 를 반환한다.
     * 클라이언트가 URI 를 직접 넘기지 않고 서버 설정에서만 선택하므로
     * 인가 코드 탈취(open redirect) 위험이 없다.
     */
    public String redirectUriFor(PlatformType platform) {
        if (platform == PlatformType.WEB) {
            if (webRedirectUri == null || webRedirectUri.isBlank()) {
                throw new GeneralException(ErrorStatus.KAKAO_WEB_NOT_CONFIGURED);
            }
            return webRedirectUri;
        }
        return redirectUri;
    }
}
