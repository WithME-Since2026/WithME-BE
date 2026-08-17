package yooze.withme.domain.auth.dto.response;

/**
 * 카카오 OAuth 로그인 시작 시 프론트에 반환하는 state + 로그인 URL.
 * 프론트는 kakaoLoginUrl 로 리다이렉트하고, 카카오가 돌려준 code·state를
 * /kakao/callback 으로 그대로 전달한다.
 */
public record KakaoStateResponse(
        String state,
        String kakaoLoginUrl
) {
}
