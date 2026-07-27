package yooze.withme.domain.auth.dto.response;

import yooze.withme.domain.auth.entity.User;

public record ProfileResponse(
        Long userId,
        String nickname,
        String profileImg
) {

    public static ProfileResponse from(User user) {
        return new ProfileResponse(user.getUserId(), user.getNickname(), user.getProfileImg());
    }
}
