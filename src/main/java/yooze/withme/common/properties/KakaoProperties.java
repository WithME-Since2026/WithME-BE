package yooze.withme.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kakao")
public record KakaoProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        /** 회원 탈퇴 시 연결 끊기(unlink)에 쓰는 Admin 키. 비어 있으면 unlink 를 건너뛴다 */
        String adminKey
) {
}
