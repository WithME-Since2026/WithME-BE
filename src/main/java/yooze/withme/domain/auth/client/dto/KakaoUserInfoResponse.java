package yooze.withme.domain.auth.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * 카카오 사용자 정보 조회 API(GET https://kapi.kakao.com/v2/user/me) 응답
 * 필요한 필드만 매핑하고 나머지는 무시한다.
 *
 * Spring Boot 4.1의 Jackson 3(tools.jackson.*) 기준 @JsonNaming/PropertyNamingStrategies 패키지 사용.
 * (KakaoTokenResponse 참고)
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoUserInfoResponse(
        Long id,
        KakaoAccount kakaoAccount
) {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KakaoAccount(
            String email,
            Profile profile
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Profile(
            String nickname,
            String profileImageUrl
    ) {
    }

    /** 이메일 안전 추출 (동의 안 했으면 kakaoAccount 자체가 없을 수 있음) */
    public String extractEmail() {
        return kakaoAccount != null ? kakaoAccount.email() : null;
    }

    /** 닉네임 안전 추출 */
    public String extractNickname() {
        return kakaoAccount != null && kakaoAccount.profile() != null
                ? kakaoAccount.profile().nickname()
                : null;
    }

    /** 프로필 이미지 URL 안전 추출 */
    public String extractProfileImage() {
        return kakaoAccount != null && kakaoAccount.profile() != null
                ? kakaoAccount.profile().profileImageUrl()
                : null;
    }
}
