package yooze.withme.domain.auth.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * 카카오 OAuth 토큰 발급 API(POST https://kauth.kakao.com/oauth/token) 응답
 * 필요한 필드만 매핑하고 나머지는 무시한다.
 *
 * Spring Boot 4.1은 RestClient/MVC 메시지 변환에 Jackson 3(tools.jackson.*)을 사용한다.
 * @JsonNaming/PropertyNamingStrategies는 databind 하위 어노테이션이라 Jackson 2와 3의
 * 패키지가 다르므로 반드시 tools.jackson.databind.* 를 사용해야 한다.
 * (@JsonIgnoreProperties 등 com.fasterxml.jackson.annotation 패키지는 2.x/3.x 공용이라 그대로 사용)
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoTokenResponse(
        String tokenType,
        String accessToken,
        String refreshToken,
        Long expiresIn,
        Long refreshTokenExpiresIn
) {
}
