package yooze.withme.domain.auth.dto.response;

import yooze.withme.domain.auth.entity.User;

public record KakaoLoginResponse(
        Long userId,
        String nickname,
        String accessToken,
        String refreshToken,
        boolean newUser
) {

    public static KakaoLoginResponse of(User user, String accessToken, String refreshToken, boolean newUser) {
        return new KakaoLoginResponse(user.getUserId(), user.getNickname(), accessToken, refreshToken, newUser);
    }
}
