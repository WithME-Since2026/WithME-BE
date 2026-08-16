package yooze.withme.domain.auth.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/**
 * 카카오 사용자 정보 조회 API(GET https://kapi.kakao.com/v2/user/me) 응답
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
            Boolean isEmailVerified,
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

    /** 이메일 인증 여부 (미인증 이메일은 본인 소유를 보장할 수 없음) */
    public boolean isEmailVerified() {
        return kakaoAccount != null && Boolean.TRUE.equals(kakaoAccount.isEmailVerified());
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
