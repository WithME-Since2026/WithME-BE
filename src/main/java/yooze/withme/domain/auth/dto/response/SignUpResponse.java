package yooze.withme.domain.auth.dto.response;

import yooze.withme.domain.auth.entity.User;

public record SignUpResponse(
        Long userId,
        String nickname,
        String accessToken,
        String refreshToken
) {

    public static SignUpResponse of(User user, String accessToken, String refreshToken) {
        return new SignUpResponse(user.getUserId(), user.getNickname(), accessToken, refreshToken);
    }
}
